-- ============================================================
-- ENTREGA 2: SEEDER DE DATOS ESPACIALES DE EJEMPLO
-- Ejecutar DESPUÉS de schema_postgis.sql y logic_postgis.sql
-- (y después de haber corrido seeder.sql / seeder_base.sql)
-- ============================================================

-- 1. Campo de tiro oficial (polígono rectangular de ejemplo, ~100x60 m aprox.)
INSERT INTO fields (name, boundary) VALUES
('Campo Nacional de Tiro con Arco',
 ST_GeomFromText('POLYGON((
    -70.6500 -33.4500,
    -70.6480 -33.4500,
    -70.6480 -33.4520,
    -70.6500 -33.4520,
    -70.6500 -33.4500
 ))', 4326));

-- Asociar el torneo activo (id_tournament = 1) a este campo
UPDATE tournaments SET id_field = 1 WHERE id_tournament = 1;

-- 2. Zona de competencia (línea de tiro), un polígono más pequeño dentro del campo
INSERT INTO competition_zones (id_tournament, name, zone_polygon) VALUES
(1, 'Línea de tiro A',
 ST_GeomFromText('POLYGON((
    -70.6498 -33.4502,
    -70.6490 -33.4502,
    -70.6490 -33.4506,
    -70.6498 -33.4506,
    -70.6498 -33.4502
 ))', 4326));

-- 3. Blancos (uno por categoría, dentro del campo)
INSERT INTO targets (id_tournament, id_category, label, location, required_distance_m) VALUES
(1, 1, 'Blanco Largo 1',    ST_SetSRID(ST_MakePoint(-70.6485, -33.4515), 4326), 70.00),
(1, 2, 'Blanco Recurvo 1',  ST_SetSRID(ST_MakePoint(-70.6486, -33.4514), 4326), 70.00),
(1, 3, 'Blanco Compuesto 1',ST_SetSRID(ST_MakePoint(-70.6487, -33.4513), 4326), 50.00);

-- 4. Zonas ambientales (viento fuerte en un sector, lluvia leve en otro)
INSERT INTO environmental_zones (id_tournament, zone_type, intensity, zone_polygon) VALUES
(1, 'VIENTO', 'FUERTE',
 ST_GeomFromText('POLYGON((
    -70.6490 -33.4510,
    -70.6480 -33.4510,
    -70.6480 -33.4520,
    -70.6490 -33.4520,
    -70.6490 -33.4510
 ))', 4326)),
(1, 'LLUVIA', 'LEVE',
 ST_GeomFromText('POLYGON((
    -70.6500 -33.4500,
    -70.6490 -33.4500,
    -70.6490 -33.4510,
    -70.6500 -33.4510,
    -70.6500 -33.4500
 ))', 4326));

-- ============================================================
-- 5. EJEMPLOS DE USO (registro de rondas con ubicación)
-- ============================================================

-- Caso OK: arquero 1 dentro de la zona de competencia y del campo,
-- asignado al blanco 1, lejos de otros arqueros.
CALL registrar_puntuacion_ronda(
    1, 1, 3,
    ARRAY[9,10,10,8,9,10],
    -70.6494, -33.4504,   -- lon, lat dentro de competition_zones
    1                       -- id_target
);

-- Caso que debería FALLAR: arquero 2 fuera de la zona de competencia
-- (coordenadas fuera del polígono definido arriba)
-- CALL registrar_puntuacion_ronda(1, 2, 3, ARRAY[8,8,9,9,8,8], -70.6000, -33.4000, 2);

-- Caso que debería FALLAR: arquero 3 a menos de 5 metros del arquero 1
-- (misma coordenada casi exacta que el arquero 1)
-- CALL registrar_puntuacion_ronda(1, 3, 3, ARRAY[7,8,9,7,8,9], -70.64940001, -33.45040001, 1);

-- Verificar distancia real arquero-blanco vs normativa:
-- SELECT * FROM fn_verificar_distancia_normativa(
--     (SELECT id_round FROM rounds WHERE id_tournament=1 AND id_archer=1 AND round_number=3)
-- );
