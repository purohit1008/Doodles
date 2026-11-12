-- =========================================================
-- DOODLES - DATABASE SETUP
-- 50% IMPLEMENTATION
-- =========================================================

-- Remove old database completely
DROP DATABASE IF EXISTS doodles_db;

-- Create fresh database
CREATE DATABASE doodles_db;

USE doodles_db;


-- =========================================================
-- PLAYERS
-- =========================================================

CREATE TABLE players (
                         player_id INT AUTO_INCREMENT PRIMARY KEY,
                         player_name VARCHAR(100) NOT NULL UNIQUE,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =========================================================
-- GAME SESSIONS
-- =========================================================

CREATE TABLE game_sessions (
                               game_id INT AUTO_INCREMENT PRIMARY KEY,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);


-- =========================================================
-- DRAWINGS
-- =========================================================

CREATE TABLE drawings (
                          drawing_id INT AUTO_INCREMENT PRIMARY KEY,

                          game_id INT NOT NULL,

                          player_id INT NOT NULL,

                          drawing_data TEXT NOT NULL,

                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                          FOREIGN KEY (game_id)
                              REFERENCES game_sessions(game_id)
                              ON DELETE CASCADE,

                          FOREIGN KEY (player_id)
                              REFERENCES players(player_id)
                              ON DELETE CASCADE
);


-- =========================================================
-- CHAT MESSAGES
-- =========================================================

CREATE TABLE chat_messages (
                               message_id INT AUTO_INCREMENT PRIMARY KEY,

                               game_id INT NOT NULL,

                               player_id INT NOT NULL,

                               message TEXT NOT NULL,

                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                               FOREIGN KEY (game_id)
                                   REFERENCES game_sessions(game_id)
                                   ON DELETE CASCADE,

                               FOREIGN KEY (player_id)
                                   REFERENCES players(player_id)
                                   ON DELETE CASCADE
);


-- =========================================================
-- TEST DATA
-- =========================================================

INSERT INTO players (player_name)
VALUES
    ('Player 1'),
    ('Player 2');


INSERT INTO game_sessions (status)
VALUES
    ('ACTIVE');


-- =========================================================
-- VERIFY DATABASE
-- =========================================================

SELECT 'DOODLES DATABASE READY' AS message;

SHOW TABLES;