-- Add parent_committee_id column to sub_committee table
ALTER TABLE sub_committee ADD COLUMN IF NOT EXISTS parent_committee_id BIGINT;

-- Add foreign key constraint
ALTER TABLE sub_committee 
ADD CONSTRAINT fk_subcommittee_parent_committee 
FOREIGN KEY (parent_committee_id) REFERENCES committee(id) ON DELETE CASCADE;

-- Update existing subcommittees to have a default parent committee
-- For now, we'll set them to committee ID 1 (assuming it exists)
UPDATE sub_committee SET parent_committee_id = 1 WHERE parent_committee_id IS NULL;

-- Make the column NOT NULL after setting default values
ALTER TABLE sub_committee ALTER COLUMN parent_committee_id SET NOT NULL;

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_subcommittee_parent_committee ON sub_committee(parent_committee_id);
