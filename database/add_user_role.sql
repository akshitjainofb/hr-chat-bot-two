-- Add role column to users table
ALTER TABLE users
ADD COLUMN role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER';

-- Update existing users to have USER role (this is already the default)
UPDATE users SET role = 'USER' WHERE role IS NULL;


