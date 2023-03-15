ALTER TABLE account_utilizations
ADD COLUMN taxable_amount_loc numeric(13, 4) NOT null default 0;

ALTER TABLE account_utilizations
ADD COLUMN tagged_settlement_id TEXT;

ALTER TABLE account_utilizations
ADD COLUMN is_draft bool NOT NULL DEFAULT false;

ALTER TABLE settlements
ADD COLUMN is_draft bool NOT NULL DEFAULT false;

CREATE TABLE "settlement_tagged_mappings" (
     "id" BIGSERIAL PRIMARY KEY,
     "settlement_id" BIGINT NOT NULL,
     "utilized_settlement_id" BIGINT NOT NULL,
     FOREIGN KEY ("settlement_id" ) REFERENCES SETTLEMENTS(ID),
     FOREIGN KEY ("utilized_settlement_id" ) REFERENCES SETTLEMENTS(ID),
     "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     "deleted_at" TIMESTAMP DEFAULT NULL
 );