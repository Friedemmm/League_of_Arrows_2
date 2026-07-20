-- ============================================================
-- ENTREGA 2: EXTENSIÓN GEOESPACIAL (PostGIS)
-- Ejecutar DESPUÉS de schema.sql (Entrega 1)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- ============================================================
-- 1. CAMPO DE TIRO OFICIAL (polígono del recinto completo)
-- ============================================================
CREATE TABLE IF NOT EXISTS fields (
    id_field BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    boundary GEOMETRY(Polygon, 4326) NOT NULL
);

-- Un torneo se disputa en un campo oficial
ALTER TABLE tournaments
    ADD COLUMN IF NOT EXISTS id_field BIGINT REFERENCES fields(id_field);

-- ============================================================
-- 2. ZONA DE COMPETENCIA (polígono donde puede pararse el arquero)
--    Puede haber varias por torneo (ej. distintas líneas de tiro)
-- ============================================================
CREATE TABLE IF NOT EXISTS competition_zones (
    id_zone BIGSERIAL PRIMARY KEY,
    id_tournament BIGINT NOT NULL REFERENCES tournaments(id_tournament) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    zone_polygon GEOMETRY(Polygon, 4326) NOT NULL
);

-- ============================================================
-- 3. BLANCOS (targets)
-- ============================================================
CREATE TABLE IF NOT EXISTS targets (
    id_target BIGSERIAL PRIMARY KEY,
    id_tournament BIGINT NOT NULL REFERENCES tournaments(id_tournament) ON DELETE CASCADE,
    id_category BIGINT NOT NULL REFERENCES categories(id_category),
    label VARCHAR(50),
    location GEOMETRY(Point, 4326) NOT NULL,
    required_distance_m NUMERIC(6,2) NOT NULL -- normativa de distancia de la categoría
);

-- ============================================================
-- 4. ZONAS AMBIENTALES (viento, lluvia, frío...) por torneo
-- ============================================================
CREATE TABLE IF NOT EXISTS environmental_zones (
    id_env_zone BIGSERIAL PRIMARY KEY,
    id_tournament BIGINT NOT NULL REFERENCES tournaments(id_tournament) ON DELETE CASCADE,
    zone_type VARCHAR(30) NOT NULL CHECK (zone_type IN ('VIENTO','LLUVIA','FRIO','CALOR','OTRO')),
    intensity VARCHAR(20) CHECK (intensity IN ('LEVE','MODERADO','FUERTE')),
    zone_polygon GEOMETRY(Polygon, 4326) NOT NULL
);

-- ============================================================
-- 5. UBICACIÓN DEL ARQUERO Y BLANCO ASIGNADO EN LA RONDA
--    (se registra al momento de iniciar la ronda, antes de las flechas)
-- ============================================================
ALTER TABLE rounds
    ADD COLUMN IF NOT EXISTS archer_location GEOMETRY(Point, 4326),
    ADD COLUMN IF NOT EXISTS id_target BIGINT REFERENCES targets(id_target);

-- ============================================================
-- 6. ÍNDICES ESPACIALES GIST (obligatorio según requerimientos)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_fields_boundary            ON fields USING GIST (boundary);
CREATE INDEX IF NOT EXISTS idx_competition_zones_polygon   ON competition_zones USING GIST (zone_polygon);
CREATE INDEX IF NOT EXISTS idx_targets_location            ON targets USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_rounds_archer_location      ON rounds USING GIST (archer_location);
CREATE INDEX IF NOT EXISTS idx_environmental_zones_polygon ON environmental_zones USING GIST (zone_polygon);
