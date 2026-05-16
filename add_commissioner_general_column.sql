-- Add commissioner_general column to country_committee_member table
-- This allows explicit selection of COMMISSIONER_GENERAL role

ALTER TABLE country_committee_member 
ADD COLUMN IF NOT EXISTS commissioner_general BOOLEAN DEFAULT FALSE;

-- Update existing records to set commissioner_general = true for members who don't have any other role
UPDATE country_committee_member 
SET commissioner_general = TRUE 
WHERE chair = FALSE 
  AND vice_chair = FALSE 
  AND committee_secretary = FALSE 
  AND committee_member = FALSE;

-- Verify the column was added
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'country_committee_member' 
  AND column_name = 'commissioner_general';
