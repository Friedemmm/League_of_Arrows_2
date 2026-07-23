-- ============================================================
-- ENTREGA 2: LÓGICA GEOESPACIAL (PostGIS)
-- Ejecutar DESPUÉS de logic.sql y schema_postgis.sql
-- ============================================================

-- ============================================================
-- 0. Ampliar el SP de registro de ronda para capturar ubicación
--    y blanco asignado (retrocompatible: parámetros con DEFAULT NULL)
-- ============================================================
DROP PROCEDURE IF EXISTS registrar_puntuacion_ronda(bigint, bigint, integer, integer[]);
DROP PROCEDURE IF EXISTS registrar_puntuacion_ronda(bigint, bigint, integer, integer[], double precision, double precision, bigint);

CREATE OR REPLACE PROCEDURE registrar_puntuacion_ronda(
    p_tournament_id BIGINT,
    p_archer_id BIGINT,
    p_round_number INT,
    p_scores INTEGER[],
    p_lon DOUBLE PRECISION DEFAULT NULL,   -- longitud GPS del arquero
    p_lat DOUBLE PRECISION DEFAULT NULL,   -- latitud GPS del arquero
    p_target_id BIGINT DEFAULT NULL        -- blanco asignado
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_is_active BOOLEAN;
    v_round_id BIGINT;
    v_score INTEGER;
    v_arrow_index INT := 1;
    v_location GEOMETRY;
BEGIN
    SELECT is_active INTO v_is_active FROM tournaments WHERE id_tournament = p_tournament_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Torneo con id % no existe.', p_tournament_id;
    END IF;

    IF NOT v_is_active THEN
        RAISE EXCEPTION 'El torneo % no está activo.', p_tournament_id;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM archers WHERE id_archer = p_archer_id) THEN
        RAISE EXCEPTION 'Arquero con id % no existe.', p_archer_id;
    END IF;

    IF p_lon IS NOT NULL AND p_lat IS NOT NULL THEN
        v_location := ST_SetSRID(ST_MakePoint(p_lon, p_lat), 4326);
    END IF;

    SELECT id_round INTO v_round_id
    FROM rounds
    WHERE id_tournament = p_tournament_id
      AND id_archer = p_archer_id
      AND round_number = p_round_number
    LIMIT 1;

    IF NOT FOUND THEN
        -- El INSERT/UPDATE de esta fila es lo que dispara los triggers espaciales
        INSERT INTO rounds (id_tournament, id_archer, round_number, archer_location, id_target)
        VALUES (p_tournament_id, p_archer_id, p_round_number, v_location, p_target_id)
        RETURNING id_round INTO v_round_id;
    ELSIF v_location IS NOT NULL THEN
        UPDATE rounds
        SET archer_location = v_location, id_target = COALESCE(p_target_id, id_target)
        WHERE id_round = v_round_id;
    END IF;

    IF EXISTS (SELECT 1 FROM arrows WHERE id_round = v_round_id) THEN
        RAISE EXCEPTION 'Las flechas de la ronda % para el arquero % ya fueron registradas.', p_round_number, p_archer_id;
    END IF;

    FOREACH v_score IN ARRAY p_scores
    LOOP
        INSERT INTO arrows (id_round, arrow_number, score)
        VALUES (v_round_id, v_arrow_index, v_score);
        v_arrow_index := v_arrow_index + 1;
    END LOOP;

    RAISE NOTICE 'Ronda % registrada para arquero % en torneo %.', p_round_number, p_archer_id, p_tournament_id;
END;
$$;

-- ============================================================
-- 1. TRIGGER — Zona de Competencia
--    La posición del arquero debe estar CONTENIDA en al menos
--    una zona de competencia definida para ese torneo.
-- ============================================================
CREATE OR REPLACE FUNCTION fn_validar_zona_competencia()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_dentro BOOLEAN;
BEGIN
    -- Si aún no se conoce la ubicación (ej. torneos sin componente espacial), no bloqueamos
    IF NEW.archer_location IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM competition_zones cz
        WHERE cz.id_tournament = NEW.id_tournament
          AND ST_Contains(cz.zone_polygon, NEW.archer_location)
    ) INTO v_dentro;

    IF NOT v_dentro THEN
        RAISE EXCEPTION 'El arquero % no se encuentra dentro de la Zona de Competencia del torneo %.',
            NEW.id_archer, NEW.id_tournament;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validar_zona_competencia ON rounds;
CREATE TRIGGER trg_validar_zona_competencia
BEFORE INSERT OR UPDATE OF archer_location ON rounds
FOR EACH ROW
EXECUTE FUNCTION fn_validar_zona_competencia();

-- ============================================================
-- 2. TRIGGER — Geocercado (campo oficial + torneo activo)
--    El puntaje solo es válido si:
--      a) el torneo está activo
--      b) el POINT del arquero está contenido en el polígono
--         oficial del campo de tiro (ST_Contains)
-- ============================================================
CREATE OR REPLACE FUNCTION fn_validar_geocerca_campo()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_boundary GEOMETRY;
    v_activo BOOLEAN;
BEGIN
    IF NEW.archer_location IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT f.boundary, t.is_active
    INTO v_boundary, v_activo
    FROM tournaments t
    LEFT JOIN fields f ON f.id_field = t.id_field
    WHERE t.id_tournament = NEW.id_tournament;

    IF NOT v_activo THEN
        RAISE EXCEPTION 'El torneo % no está activo: el puntaje no es válido.', NEW.id_tournament;
    END IF;

    -- Si el torneo no tiene campo oficial asociado en la tabla fields,
    -- omitir esta validación (fn_validar_zona_competencia ya valida
    -- que el arquero esté dentro de la competition_zone dibujada por el admin)
    IF v_boundary IS NULL THEN RETURN NEW; END IF;

    IF NOT ST_Contains(v_boundary, NEW.archer_location) THEN
        RAISE EXCEPTION 'Puntaje inválido: el arquero % está fuera del campo de tiro oficial.', NEW.id_archer;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validar_geocerca_campo ON rounds;
CREATE TRIGGER trg_validar_geocerca_campo
BEFORE INSERT OR UPDATE OF archer_location ON rounds
FOR EACH ROW
EXECUTE FUNCTION fn_validar_geocerca_campo();

-- ============================================================
-- 3. TRIGGER — Seguridad: distancia mínima entre arqueros activos
--    Si dos arqueros del mismo torneo (activo) quedan a menos
--    de 5 metros entre sí, se bloquea el registro.
-- ============================================================
CREATE OR REPLACE FUNCTION fn_validar_seguridad_distancia()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_conflicto RECORD;
BEGIN
    IF NEW.archer_location IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT r.id_archer,
           ST_Distance(r.archer_location::geography, NEW.archer_location::geography) AS distancia
    INTO v_conflicto
    FROM rounds r
    JOIN tournaments t ON t.id_tournament = r.id_tournament
    WHERE r.id_tournament = NEW.id_tournament
      AND r.id_archer <> NEW.id_archer
      AND r.archer_location IS NOT NULL
      AND t.is_active = TRUE
      AND ST_Distance(r.archer_location::geography, NEW.archer_location::geography) < 5
    ORDER BY distancia ASC
    LIMIT 1;

    IF FOUND THEN
        RAISE EXCEPTION 'Ubicación insegura: el arquero % está a % m del arquero % (mínimo permitido: 5 m).',
            NEW.id_archer, ROUND(v_conflicto.distancia::numeric, 2), v_conflicto.id_archer;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validar_seguridad_distancia ON rounds;
CREATE TRIGGER trg_validar_seguridad_distancia
BEFORE INSERT OR UPDATE OF archer_location ON rounds
FOR EACH ROW
EXECUTE FUNCTION fn_validar_seguridad_distancia();

-- ============================================================
-- 4. FUNCIÓN — Distancia real arquero-blanco vs normativa de categoría
--    (equivalente al "Procedimiento Almacenado" del enunciado;
--    se implementa como FUNCTION para poder retornar el resultado
--    directamente a la API sin necesidad de cursores)
-- ============================================================
CREATE OR REPLACE FUNCTION fn_verificar_distancia_normativa(p_round_id BIGINT)
RETURNS TABLE (
    distancia_real_m NUMERIC,
    distancia_normativa_m NUMERIC,
    cumple_normativa BOOLEAN
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_archer_loc GEOMETRY;
    v_target_loc GEOMETRY;
    v_normativa NUMERIC;
    v_distancia NUMERIC;
    v_tolerancia NUMERIC := 1.0; -- tolerancia de 1 metro
BEGIN
    SELECT r.archer_location, tg.location, tg.required_distance_m
    INTO v_archer_loc, v_target_loc, v_normativa
    FROM rounds r
    JOIN targets tg ON tg.id_target = r.id_target
    WHERE r.id_round = p_round_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La ronda % no tiene blanco o ubicación asignados.', p_round_id;
    END IF;

    v_distancia := ST_Distance(v_archer_loc::geography, v_target_loc::geography);

    RETURN QUERY
    SELECT ROUND(v_distancia, 2),
           v_normativa,
           (ABS(v_distancia - v_normativa) <= v_tolerancia);
END;
$$;

-- Ejemplo de uso:
-- SELECT * FROM fn_verificar_distancia_normativa(1);

-- ============================================================
-- 5. ANÁLISIS ESPACIAL DE DESEMPEÑO
--    Correlación entre condición ambiental (zona de viento/lluvia/frío
--    que contiene al blanco) y la precisión del arquero (promedio
--    de puntaje por flecha en esa ronda).
-- ============================================================
CREATE OR REPLACE VIEW vw_desempeno_por_condicion_ambiental AS
SELECT
    ez.zone_type              AS condicion_ambiental,
    ez.intensity              AS intensidad,
    ar.id_archer,
    ar.name                   AS arquero,
    r.id_round,
    AVG(a.score)              AS precision_promedio_flecha
FROM rounds r
JOIN targets tg           ON tg.id_target = r.id_target
JOIN arrows a              ON a.id_round = r.id_round
JOIN archers ar            ON ar.id_archer = r.id_archer
JOIN environmental_zones ez ON ez.id_tournament = r.id_tournament
                            AND ST_Contains(ez.zone_polygon, tg.location)
GROUP BY ez.zone_type, ez.intensity, ar.id_archer, ar.name, r.id_round;

-- Consulta de correlación: compara el promedio de precisión
-- entre distintas condiciones ambientales (ej. viento fuerte vs sin viento)
-- y calcula el coeficiente de correlación de Pearson entre "intensidad"
-- (convertida a escala numérica) y la precisión.
SELECT
    condicion_ambiental,
    intensidad,
    COUNT(*)                                  AS rondas_evaluadas,
    ROUND(AVG(precision_promedio_flecha)::numeric, 3) AS promedio_precision
FROM vw_desempeno_por_condicion_ambiental
GROUP BY condicion_ambiental, intensidad
ORDER BY condicion_ambiental, intensidad;

-- Coeficiente de correlación de Pearson (usando corr() nativo de PostgreSQL)
-- entre intensidad ambiental (mapeada a 1=LEVE, 2=MODERADO, 3=FUERTE) y precisión.
SELECT
    condicion_ambiental,
    corr(
        CASE intensidad WHEN 'LEVE' THEN 1 WHEN 'MODERADO' THEN 2 WHEN 'FUERTE' THEN 3 END,
        precision_promedio_flecha
    ) AS coeficiente_correlacion
FROM vw_desempeno_por_condicion_ambiental
GROUP BY condicion_ambiental;
