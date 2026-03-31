-- 1. POBLAR CATEGORÍAS
INSERT INTO categorias (nombre, puntos, tipo_de_torneo) VALUES ('Grand Slam', 2000, 'MAJOR');
INSERT INTO categorias (nombre, puntos, tipo_de_torneo) VALUES ('Masters 1000', 1000, 'M1000');
INSERT INTO categorias (nombre, puntos, tipo_de_torneo) VALUES ('ATP 500', 500, 'ATP500');

-- 2. POBLAR JUGADORES
INSERT INTO jugadores (nombre, ranking_actual, nacionalidad) VALUES ('Jannik Sinner', 1, 'IT');
INSERT INTO jugadores (nombre, ranking_actual, nacionalidad) VALUES ('Carlos Alcaraz', 2, 'ES');
INSERT INTO jugadores (nombre, ranking_actual, nacionalidad) VALUES ('Sebastián Báez', 52, 'AR');
INSERT INTO jugadores (nombre, ranking_actual, nacionalidad) VALUES ('Francisco Cerúndolo', 19, 'AR');
INSERT INTO jugadores (nombre, ranking_actual, nacionalidad) VALUES ('Novak Djokovic', 3, 'RS');