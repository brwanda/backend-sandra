-- Check for foreign key constraints on committee table
SELECT 
    tc.table_name, 
    kcu.column_name, 
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name 
FROM 
    information_schema.table_constraints AS tc 
    JOIN information_schema.key_column_usage AS kcu
      ON tc.constraint_name = kcu.constraint_name
      AND tc.table_schema = kcu.table_schema
    JOIN information_schema.constraint_column_usage AS ccu
      ON ccu.constraint_name = tc.constraint_name
      AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY' 
    AND (ccu.table_name = 'committee' OR tc.table_name = 'committee');

-- Check sub_committee table structure
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'sub_committee';

-- Check if there are any subcommittees that reference committee ID 2
SELECT * FROM sub_committee WHERE id = 2;

-- Check if there are any users that reference committee ID 2
SELECT * FROM users WHERE id = 2;

-- Check if there are any other tables that might reference committee
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
    AND table_name LIKE '%committee%';
