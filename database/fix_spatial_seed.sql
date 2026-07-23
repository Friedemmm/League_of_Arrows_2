-- ============================================================
-- FIX ESPACIAL COMPLETO — ejecutar con:
--   sudo docker compose exec -T db psql -U loa_user -d league_of_arrows -f /tmp/fix_spatial_seed.sql
-- (copiar primero: sudo docker compose cp database/fix_spatial_seed.sql db:/tmp/)
-- ============================================================

-- 1. Ampliar el campo oficial a ~600×550 m
UPDATE fields
SET boundary = ST_GeomFromText('POLYGON((
    -70.6530 -33.4480,
    -70.6450 -33.4480,
    -70.6450 -33.4540,
    -70.6530 -33.4540,
    -70.6530 -33.4480
))', 4326)
WHERE id_field = 1;

-- Si no existía campo 1, crearlo
INSERT INTO fields (name, boundary)
SELECT 'Campo Principal Liga de Flechas',
       ST_GeomFromText('POLYGON((
           -70.6530 -33.4480,
           -70.6450 -33.4480,
           -70.6450 -33.4540,
           -70.6530 -33.4540,
           -70.6530 -33.4480
       ))', 4326)
WHERE NOT EXISTS (SELECT 1 FROM fields WHERE id_field = 1);

-- Vincular torneo 1 al campo
UPDATE tournaments SET id_field = 1 WHERE id_tournament = 1;

-- 2. Reemplazar zona de competencia del torneo 1 por una amplia
DELETE FROM competition_zones WHERE id_tournament = 1;
INSERT INTO competition_zones (id_tournament, name, zone_polygon)
VALUES (
  1, 'Zona de Competencia Principal',
  ST_GeomFromText('POLYGON((
      -70.6520 -33.4485,
      -70.6460 -33.4485,
      -70.6460 -33.4535,
      -70.6520 -33.4535,
      -70.6520 -33.4485
  ))', 4326)
);

-- 3. Desvincular blancos de las rondas para poder borrarlos
UPDATE rounds SET id_target = NULL WHERE id_tournament = 1;

-- 4. Reemplazar blancos del torneo 1 (uno por categoría, dentro del campo)
DELETE FROM targets WHERE id_tournament = 1;
INSERT INTO targets (id_tournament, id_category, label, location, required_distance_m) VALUES
(1, 1, 'Blanco Largo',       ST_SetSRID(ST_MakePoint(-70.6515, -33.4496), 4326), 70.00),
(1, 2, 'Blanco Recurvo',     ST_SetSRID(ST_MakePoint(-70.6505, -33.4496), 4326), 70.00),
(1, 3, 'Blanco Compuesto',   ST_SetSRID(ST_MakePoint(-70.6495, -33.4496), 4326), 50.00),
(1, 4, 'Blanco Tradicional', ST_SetSRID(ST_MakePoint(-70.6485, -33.4496), 4326), 50.00);

-- 5. Asignar posiciones a arqueros del torneo 1
--    Cada posición está dentro del campo+zona y separada >5 m de las demás
--    0.0001° lat ≈ 11.1 m (suficientemente lejos)
UPDATE rounds SET archer_location = ST_SetSRID(ST_MakePoint(-70.6515, -33.4490), 4326)
WHERE id_tournament = 1 AND id_archer = 1;

UPDATE rounds SET archer_location = ST_SetSRID(ST_MakePoint(-70.6505, -33.4490), 4326)
WHERE id_tournament = 1 AND id_archer = 2;

UPDATE rounds SET archer_location = ST_SetSRID(ST_MakePoint(-70.6495, -33.4490), 4326)
WHERE id_tournament = 1 AND id_archer = 3;

-- 6. Verificación final
SELECT
  r.id_round,
  r.id_archer,
  ST_Contains(f.boundary, r.archer_location)     AS dentro_campo,
  ST_Contains(cz.zone_polygon, r.archer_location) AS dentro_zona
FROM rounds r
JOIN tournaments t   ON t.id_tournament  = r.id_tournament
JOIN fields f        ON f.id_field       = t.id_field
JOIN competition_zones cz ON cz.id_tournament = r.id_tournament
WHERE r.id_tournament = 1 AND r.archer_location IS NOT NULL;

-- Área del campo y zona
SELECT 'campo' AS tipo, ROUND(ST_Area(boundary::geography)::numeric) AS area_m2
FROM fields WHERE id_field = 1
UNION ALL
SELECT 'zona', ROUND(ST_Area(zone_polygon::geography)::numeric)
FROM competition_zones WHERE id_tournament = 1;
