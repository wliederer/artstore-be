-- Manual migration to add phone column to orders table
-- Run this if Hibernate doesn't automatically add the column

-- Add phone column to orders table
ALTER TABLE orders ADD COLUMN phone VARCHAR(20);

-- Verify the change
SELECT column_name, data_type, character_maximum_length, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'orders' 
AND column_name = 'phone';