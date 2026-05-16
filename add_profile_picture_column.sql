-- Add profile_picture column to users table
-- Run this script to add profile picture support to existing users

-- Add the profile_picture column
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS profile_picture VARCHAR(500);

-- Add a comment to describe the column
COMMENT ON COLUMN users.profile_picture IS 'URL or path to the user profile picture';

-- Create an index for better performance when querying by profile picture
CREATE INDEX IF NOT EXISTS idx_users_profile_picture ON users(profile_picture);

-- Update existing users to have a default profile picture (optional)
-- Uncomment the following line if you want to set a default profile picture for existing users
-- UPDATE users SET profile_picture = NULL WHERE profile_picture IS NULL;

-- Verify the column was added
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'users' AND column_name = 'profile_picture';

-- Show the updated table structure
\d users;
