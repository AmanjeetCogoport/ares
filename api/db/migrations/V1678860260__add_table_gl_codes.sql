CREATE TABLE PUBLIC.gl_codes (
    id BIGSERIAL CONSTRAINT gl_codes_PK PRIMARY KEY,
    entity_code int4 NOT NULL,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    bank_name VARCHAR(50),
    currency VARCHAR(5),
    gl_code VARCHAR(10) NOT NULL,
    bank_short_name VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO gl_codes(entity_code, account_number, bank_name, currency, gl_code, bank_short_name)
VALUES (301, '609000842058', 'RBL Bank Ltd', 'INR', 222021, 'RBLCP', NOW()),
       (301, '409001406475', 'RBL Bank Ltd', 'INR', 222013, 'RBLP', NOW()),
       (301, '201015563832', 'Indusind Bank Ltd', 'INR', 222015, 'INDC', NOW()),
       (301, '409001685863', 'RBL Bank Ltd', 'USD', 222017, 'RBLPU', NOW()),
       (101, '409000876343', 'RBL Bank Ltd', 'INR', 222001, 'RBLC', NOW()),
       (101, '609000715480', 'RBL Bank Ltd', 'INR', 314003, 'RBLD', NOW()),
       (101, '409000824933', 'RBL Bank Ltd', 'USD', 222002, 'RBLU', NOW());