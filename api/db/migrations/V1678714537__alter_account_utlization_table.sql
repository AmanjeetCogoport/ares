ALTER TABLE account_utilizations
ADD COLUMN tagged_settlement_id TEXT,
ADD COLUMN is_draft bool NOT NULL DEFAULT false,
ADD COLUMN PAYABLE_AMOUNT_LOC numeric(13, 4) NOT null default 0,
ADD COLUMN PAYABLE_AMOUNT numeric(13, 4) NOT null default 0;

ALTER TABLE settlements
ADD COLUMN is_draft bool NOT NULL DEFAULT false,
ADD COLUMN settlement_num VARCHAR(255);

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
values('SET',1,now(),now());