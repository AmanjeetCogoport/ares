ALTER TABLE account_utilizations
ADD COLUMN tagged_settlement_id TEXT,
ADD COLUMN is_void bool NOT NULL DEFAULT false,
ADD COLUMN tds_amount_loc numeric(13, 4) NOT null default 0,
ADD COLUMN tds_amount numeric(13, 4) NOT null default 0;

ALTER TABLE settlements
ADD COLUMN is_void bool NOT NULL DEFAULT false,
ADD COLUMN settlement_num VARCHAR(255);         --add uniqueness here

CREATE TABLE "settlement_tagged_mappings" (
     "id" BIGSERIAL PRIMARY KEY,
     "settlement_id" BIGINT NOT NULL,
     "utilized_settlement_id" BIGINT NOT NULL,
     FOREIGN KEY ("settlement_id" ) REFERENCES SETTLEMENTS(ID),
     FOREIGN KEY ("utilized_settlement_id" ) REFERENCES SETTLEMENTS(ID),
     "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     "deleted_at" TIMESTAMP DEFAULT NULL
 );

insert into payment_sequence_numbers(sequence_type,next_sequence_number,created_at,updated_at)
values('SETL',1,now(),now());