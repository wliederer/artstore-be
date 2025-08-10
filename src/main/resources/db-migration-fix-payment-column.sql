-- Manual migration to fix stripe_payment_intent_id column
-- Run this if Hibernate doesn't automatically update the column

-- Make stripe_payment_intent_id nullable
ALTER TABLE payments ALTER COLUMN stripe_payment_intent_id DROP NOT NULL;

-- Verify the change
SELECT column_name, is_nullable, data_type 
FROM information_schema.columns 
WHERE table_name = 'payments' 
AND column_name IN ('stripe_payment_intent_id', 'stripe_checkout_session_id');