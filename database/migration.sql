-- Migration script to add status column and update existing records
-- Run this script to fix the status field issue

-- Add the status column to the pdf_documents table
ALTER TABLE pdf_documents ADD COLUMN status VARCHAR(20) DEFAULT 'PROCESSING';

-- Update existing records based on their indexed status
UPDATE pdf_documents 
SET status = CASE 
    WHEN indexed = true THEN 'INDEXED'
    WHEN indexed = false THEN 'PROCESSING'
    ELSE 'PROCESSING'
END;

-- Make the status column NOT NULL after updating existing records
ALTER TABLE pdf_documents ALTER COLUMN status SET NOT NULL;
