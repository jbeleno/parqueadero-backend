-- ================================================================
-- Docker entrypoint: runs ONCE on first "docker-compose up"
-- Only enables PostGIS. Seed data is in Spring Boot's data.sql
-- (runs AFTER Hibernate creates the schema via ddl-auto: update)
-- ================================================================

CREATE EXTENSION IF NOT EXISTS postgis;
