ALTER TABLE payments ADD CONSTRAINT enforce_unique_payment_num_value UNIQUE (payment_num_value);
