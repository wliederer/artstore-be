-- Migration: Add orderId column to orders table for security
-- This replaces the use of incremental IDs with random UUIDs for external access

-- First, check if the column already exists
-- Add the orderId column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='orders' AND column_name='order_id') THEN
        ALTER TABLE orders ADD COLUMN order_id VARCHAR(36);
    END IF;
END $$;

-- Update existing records to have orderId values (only if they're NULL)
-- For PostgreSQL:
UPDATE orders SET order_id = gen_random_uuid()::text WHERE order_id IS NULL;

-- For MySQL:
-- UPDATE orders SET order_id = UUID() WHERE order_id IS NULL;

-- For H2 (development/testing):
-- UPDATE orders SET order_id = RANDOM_UUID() WHERE order_id IS NULL;

-- Make the column NOT NULL after populating existing data
-- Only if not already NOT NULL
DO $$
BEGIN
    -- Check if column allows NULL
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'orders' 
        AND column_name = 'order_id' 
        AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE orders ALTER COLUMN order_id SET NOT NULL;
    END IF;
END $$;

-- Add unique constraint if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'uk_orders_order_id' 
        AND table_name = 'orders'
    ) THEN
        ALTER TABLE orders ADD CONSTRAINT uk_orders_order_id UNIQUE (order_id);
    END IF;
END $$;

-- Create index for performance if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_orders_order_id' 
        AND tablename = 'orders'
    ) THEN
        CREATE INDEX idx_orders_order_id ON orders(order_id);
    END IF;
END $$;

-- Note: After running this migration, you should update your application
-- to use orderId instead of id for external API access for security reasons.