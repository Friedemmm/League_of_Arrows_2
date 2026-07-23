-- ============================================================
-- SEEDER POSTGIS — Datos espaciales de ejemplo
-- Ejecutar DESPUÉS de: schema_postgis.sql, logic_postgis.sql,
--                       y el seeder base (megaseeder)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- 1. Campo de tiro oficial — polígono amplio (~600×550 m)
--    Centro: lon=-70.6490, lat=-33.4510
--    0.001° lat ≈ 111 m  |  0.001° lon ≈ 93 m
--    → 0.006° lon × 0.005° lat ≈ 558m × 555m (sobra espacio)
-- ─────────────────────────────────────────────────────────────
INSERT INTO fields (name, boundary) VALUES
('Campo Principal Liga de Flechas',
 ST_GeomFromText('POLYGON((
     -70.6530 -33.4480,
     -70.6450 -33.4480,
     -70.6450 -33.4540,
     -70.6530 -33.4540,
     -70.6530 -33.4480
 ))', 4326));

-- Asociar el torneo 1 (Campeonato Mundial activo) a este campo
UPDATE tournaments SET id_field = 1 WHERE id_tournament = 1;

-- ─────────────────────────────────────────────────────────────
-- 2. Zona de competencia — casi todo el campo (margen de ~20 m)
-- ─────────────────────────────────────────────────────────────
INSERT INTO competition_zones (id_tournament, name, zone_polygon) VALUES
(1, 'Zona de Competencia Principal',
 ST_GeomFromText('POLYGON((
     -70.6520 -33.4485,
     -70.6460 -33.4485,
     -70.6460 -33.4535,
     -70.6520 -33.4535,
     -70.6520 -33.4485
 ))', 4326));

-- ─────────────────────────────────────────────────────────────
-- 3. Blancos — uno por categoría, dentro del campo
--    A ~70 m de la línea de tiro norte (lat=-33.4490)
--    → 70m ≈ 0.00063° lat → lat del blanco ≈ -33.4496
-- ─────────────────────────────────────────────────────────────
INSERT INTO targets (id_tournament, id_category, label, location, required_distance_m) VALUES
(1, 1, 'Blanco Largo',      ST_SetSRID(ST_MakePoint(-70.6515, -33.4496), 4326), 70.00),
(1, 2, 'Blanco Recurvo',    ST_SetSRID(ST_MakePoint(-70.6505, -33.4496), 4326), 70.00),
(1, 3, 'Blanco Compuesto',  ST_SetSRID(ST_MakePoint(-70.6495, -33.4496), 4326), 50.00),
(1, 4, 'Blanco Tradicional',ST_SetSRID(ST_MakePoint(-70.6485, -33.4496), 4326), 50.00);

-- ─────────────────────────────────────────────────────────────
-- 4. Zonas ambientales (viento / lluvia) dentro del campo
-- ─────────────────────────────────────────────────────────────
INSERT INTO environmental_zones (id_tournament, zone_type, intensity, zone_polygon) VALUES
(1, 'VIENTO', 'FUERTE',
 ST_GeomFromText('POLYGON((
     -70.6520 -33.4510,
     -70.6490 -33.4510,
     -70.6490 -33.4540,
     -70.6520 -33.4540,
     -70.6520 -33.4510
 ))', 4326)),
(1, 'LLUVIA', 'LEVE',
 ST_GeomFromText('POLYGON((
     -70.6490 -33.4480,
     -70.6450 -33.4480,
     -70.6450 -33.4510,
     -70.6490 -33.4510,
     -70.6490 -33.4480
 ))', 4326));

-- ─────────────────────────────────────────────────────────────
-- 5. Posiciones de arqueros para las rondas del torneo 1
--    Las 3 posiciones deben estar:
--      ✓ Dentro del campo  (fields.boundary)
--      ✓ Dentro de la zona (competition_zones.zone_polygon)
--      ✓ Separadas >5 m entre sí  (ST_Distance)
--
--    0.0001° lat ≈ 11.1 m → separación segura
--    Línea de tiro: lat=-33.4490 (norte del campo)
--
--    Arquero 1 → lon=-70.6515, lat=-33.4490
--    Arquero 2 → lon=-70.6505, lat=-33.4490  (≈93m al este)
--    Arquero 3 → lon=-70.6495, lat=-33.4490  (≈93m al este)
-- ─────────────────────────────────────────────────────────────
UPDATE rounds
SET archer_location = ST_SetSRID(ST_MakePoint(-70.6515, -33.4490), 4326)
WHERE id_tournament = 1 AND id_archer = 1;

-- Si hay rondas del arquero 2 o 3 en torneo 1, asignarles posición también
-- (el megaseeder solo crea rondas de arquero 1 en torneo 1, pero por si acaso)
-- UPDATE rounds SET archer_location = ST_SetSRID(ST_MakePoint(-70.6505, -33.4490), 4326)
-- WHERE id_tournament = 1 AND id_archer = 2;
-- UPDATE rounds SET archer_location = ST_SetSRID(ST_MakePoint(-70.6495, -33.4490), 4326)
-- WHERE id_tournament = 1 AND id_archer = 3;

-- ─────────────────────────────────────────────────────────────
-- 6. Verificación de que las geometrías son coherentes
-- ─────────────────────────────────────────────────────────────
-- Comprobar que la zona está dentro del campo
DO $$
DECLARE
  v_ok BOOLEAN;
BEGIN
  SELECT ST_Within(cz.zone_polygon, f.boundary) INTO v_ok
  FROM competition_zones cz
  JOIN tournaments t ON t.id_tournament = cz.id_tournament
  JOIN fields f ON f.id_field = t.id_field
  WHERE cz.id_tournament = 1 LIMIT 1;

  IF v_ok THEN
    RAISE NOTICE 'OK: La zona de competencia está contenida en el campo.';
  ELSE
    RAISE WARNING 'ADVERTENCIA: La zona de competencia NO está dentro del campo.';
  END IF;
END $$;
