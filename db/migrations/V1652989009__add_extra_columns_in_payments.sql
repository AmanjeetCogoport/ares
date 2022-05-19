alter table payments
add  account_no varchar(30);

alter table payments
drop column bank_id,drop column entity_id;