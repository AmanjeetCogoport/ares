ALTER TABLE account_utilizations DROP CONSTRAINT check_led_currency;


ALTER TABLE account_utilizations
    ADD CONSTRAINT check_led_currency
        CHECK (
                (led_currency = 'INR' AND (entity_code = 101 OR entity_code = 301)) OR
                (led_currency = 'SGD' AND entity_code = 401) OR
                (led_currency = 'EUR' AND entity_code = 201) OR
                (led_currency = 'VND' AND entity_code = 501) OR
                (led_currency = 'THB' AND entity_code = 601) OR
                (led_currency = 'IDR' AND entity_code = 701) OR
                (led_currency = 'CNY' AND entity_code = 801)
                );