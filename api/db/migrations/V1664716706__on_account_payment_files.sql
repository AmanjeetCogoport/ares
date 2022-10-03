CREATE TABLE payment_files (
   id BIGSERIAL PRIMARY KEY,
   acc_mode public."account_mode" NOT NULL,
   file_name VARCHAR(250) NOT NULL,
   file_url TEXT NOT NULL,
   error_file_url VARCHAR(100),
   total_records INT NOT NULL DEFAULT 0,
   success_records INT NOT NULL DEFAULT 0,
   created_by UUID NOT NULL,
   updated_by UUID NOT NULL,
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
