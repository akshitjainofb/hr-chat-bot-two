-- Add include_context column to chat_rooms table
ALTER TABLE chat_rooms ADD COLUMN include_context BOOLEAN NOT NULL DEFAULT TRUE;

-- Update existing chat rooms to have context enabled by default
UPDATE chat_rooms SET include_context = TRUE WHERE include_context IS NULL;
