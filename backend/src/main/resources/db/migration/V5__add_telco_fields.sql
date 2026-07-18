-- Add Telco CSV import fields

-- 1. Customers
ALTER TABLE customers
    ADD COLUMN external_id VARCHAR(255) UNIQUE,
    ADD COLUMN gender VARCHAR(20),
    ADD COLUMN is_senior BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN has_partner BOOLEAN NOT NULL DEFAULT false;

-- 2. Subscriptions
ALTER TABLE subscriptions
    ADD COLUMN months_active INTEGER;

-- 3. Payments
ALTER TABLE payments
    ADD COLUMN payment_method VARCHAR(20);
