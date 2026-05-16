-- Fix existing subcommittees to have proper parent committee relationship
-- First, ensure we have a committee (Commissioner General)
INSERT INTO committee (id, committee_name) 
VALUES (1, 'Commissioner General') 
ON CONFLICT (id) DO NOTHING;

-- Update all existing subcommittees to have the Commissioner General as their parent
UPDATE sub_committee 
SET parent_committee_id = 1 
WHERE parent_committee_id IS NULL;

-- Verify the update
SELECT 
    sc.id as subcommittee_id,
    sc.subcommittee_name,
    c.id as parent_committee_id,
    c.committee_name as parent_committee_name
FROM sub_committee sc
LEFT JOIN committee c ON sc.parent_committee_id = c.id
ORDER BY sc.id;
