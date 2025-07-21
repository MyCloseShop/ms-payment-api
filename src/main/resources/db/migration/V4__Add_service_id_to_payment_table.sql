-- Migration to add service_id column to payment table
ALTER TABLE payment ADD COLUMN service_id BINARY(16);

-- Create index for the new column
CREATE INDEX idx_service_id ON payment(service_id);