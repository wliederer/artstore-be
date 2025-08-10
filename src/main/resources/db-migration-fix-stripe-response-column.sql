-- Manual migration to fix stripe_response column length
-- Run this if Hibernate doesn't automatically update the column

-- Increase stripe_response column length from 1000 to 5000 characters
ALTER TABLE payments ALTER COLUMN stripe_response TYPE VARCHAR(5000);

-- Verify the change
SELECT column_name, character_maximum_length, data_type 
FROM information_schema.columns 
WHERE table_name = 'payments' 
AND column_name = 'stripe_response';