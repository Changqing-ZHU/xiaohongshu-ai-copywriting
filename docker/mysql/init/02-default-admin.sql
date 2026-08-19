-- Default Docker deployment administrator. The password column stores only
-- a BCrypt hash with cost factor 12; no plaintext password is persisted.
INSERT INTO users (username, password, role, created_at)
SELECT
    'admin',
    '$2a$12$6h/Ca1Gadg71ewExLfXlu.0R09uzYj7RTjR1h.NkDddwIKDb9Op/S',
    'ADMIN',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);
