INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'System administrator'),
    ('DISPATCHER', 'Dispatcher operations'),
    ('PASSENGER', 'Passenger self-service')
ON CONFLICT (name) DO NOTHING;

-- Password for seed user: ChangeMe123! (BCrypt)
INSERT INTO users (username, password_hash, enabled)
VALUES (
    'admin',
    '$2b$10$blYB.fEDaJLEEcrO1NBO3uiqC0/Rk8p6c.vCiGCxGe4I6bemnZ1OC',
    TRUE
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.username = 'admin'
ON CONFLICT DO NOTHING;
