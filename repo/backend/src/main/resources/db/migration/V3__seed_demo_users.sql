-- Demo accounts for Phase 2 RBAC (password for all: ChangeMe123!)
INSERT INTO users (username, password_hash, enabled)
VALUES
    ('dispatcher1', '$2b$10$blYB.fEDaJLEEcrO1NBO3uiqC0/Rk8p6c.vCiGCxGe4I6bemnZ1OC', TRUE),
    ('passenger1', '$2b$10$blYB.fEDaJLEEcrO1NBO3uiqC0/Rk8p6c.vCiGCxGe4I6bemnZ1OC', TRUE)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'DISPATCHER'
WHERE u.username = 'dispatcher1'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'PASSENGER'
WHERE u.username = 'passenger1'
ON CONFLICT (user_id, role_id) DO NOTHING;
