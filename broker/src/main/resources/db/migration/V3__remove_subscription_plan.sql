-- BearMQ is now fully open-source; subscription plan limits are removed.
-- 1. Drop FK constraint + column from tenant
ALTER TABLE tenant DROP CONSTRAINT IF EXISTS fk_tenant_plan;
ALTER TABLE tenant DROP COLUMN IF EXISTS subscription_plan_id;

-- 2. Drop the subscription_plan table
DROP TABLE IF EXISTS subscription_plan;
