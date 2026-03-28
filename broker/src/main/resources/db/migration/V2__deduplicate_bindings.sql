-- Remove duplicate bindings created before idempotency was enforced.
-- Keeps the oldest row (lowest ULID = earliest created_at) for each
-- (vhost_id, source_exchange_id, destination_type, destination_queue_id,
--  destination_exchange_id, routing_key) combination.
DELETE FROM binding
WHERE id NOT IN (
    SELECT DISTINCT ON (
        vhost_id,
        source_exchange_id,
        destination_type,
        COALESCE(destination_queue_id,    ''),
        COALESCE(destination_exchange_id, ''),
        COALESCE(routing_key,             '')
    ) id
    FROM binding
    ORDER BY
        vhost_id,
        source_exchange_id,
        destination_type,
        COALESCE(destination_queue_id,    ''),
        COALESCE(destination_exchange_id, ''),
        COALESCE(routing_key,             ''),
        id  -- ULID is time-sortable; lowest = oldest
);
