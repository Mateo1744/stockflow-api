INSERT INTO roles (name, description)
VALUES
    ('ADMIN', 'Full system administration'),
    ('INVENTORY_MANAGER', 'Catalog and inventory management'),
    ('SELLER', 'Sales and customer management')
ON CONFLICT (name) DO NOTHING;
