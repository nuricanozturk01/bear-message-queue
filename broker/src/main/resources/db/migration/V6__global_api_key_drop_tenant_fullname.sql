CREATE TABLE application_settings
(
    id                VARCHAR(32)  NOT NULL,
    messaging_api_key VARCHAR(64)  NOT NULL,
    CONSTRAINT pk_application_settings PRIMARY KEY (id)
);

INSERT INTO application_settings (id, messaging_api_key)
SELECT 'default',
       COALESCE(
           (SELECT api_key FROM tenant WHERE NOT deleted ORDER BY created_at LIMIT 1),
           'bearmqt-' || md5(random()::text || clock_timestamp()::text))
WHERE NOT EXISTS (SELECT 1 FROM application_settings WHERE id = 'default');

ALTER TABLE tenant DROP COLUMN IF EXISTS api_key;
ALTER TABLE tenant DROP COLUMN IF EXISTS full_name;
