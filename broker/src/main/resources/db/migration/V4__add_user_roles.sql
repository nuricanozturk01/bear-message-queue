-- Add role column; default USER for existing tenants.
-- The AdminInitializer will create the first ADMIN on startup if none exists.
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS role VARCHAR(16) NOT NULL DEFAULT 'USER';

-- If there are already tenants, mark the oldest one as ADMIN.
UPDATE tenant
SET role = 'ADMIN'
WHERE id = (SELECT id FROM tenant ORDER BY created_at LIMIT 1);
