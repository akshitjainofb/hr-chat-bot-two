#!/bin/bash

echo "Running database migration to fix PDF document statuses..."

# Run the migration endpoint
curl -X POST http://localhost:8080/api/migration/update-pdf-statuses

echo ""
echo "Migration completed!"
