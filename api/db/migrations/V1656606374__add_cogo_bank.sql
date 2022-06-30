create table cogo_bank_details
(
  id serial,
  bank_id uuid,
  account_no varchar(30),
  bank_name varchar(100),
  entity_code int,
  created_at timestamp,
  updated_at timestamp,
  primary key(id)
);

insert into cogo_bank_details(bank_id,account_no,bank_name,entity_code,created_at,updated_at)
values
('b57f12ca-6288-4706-be17-6e52c8f8743c','0-021112-003','Citibank N.A',401,now(),now()),
('15f62457-57c7-4a80-bc83-1535aa6ea021','409001406475','RBL BANK LTD',301,now(),now()),
('9e8bc776-4794-4105-bdcb-85b248093140','409001685863','RBL BANK LIMITED',301,now(),now()),
('be606f38-0429-4c48-948a-4e8f6493d9fa','201015563832','INDUSIND BANK LTD, OPERA HOUSE',301,now(),now()),
('8300e176-16d4-4e99-9108-c293f49e8ebf','609000842058','RBL BANK LTD, LOWER PAREL',301,now(),now()),
('c9218d66-b168-4752-a566-40ef4de7350f','NL 18 INGB 0670 3440 95','Ing Bank N V',201,now(),now()),
('91d3a6ff-2f9e-4d89-80ff-ae60e47bc09b','NL 92 INGB 0020 1127 69','Ing Bank N V',201,now(),now()),
('5d66d64a-9bde-4619-8fd4-bf38819f2a55','409000824933','RBL BANK LTD',101,now(),now()),
('0e19483d-15b2-4f8f-8b5e-3fe4b1c0dcf1','603014033080','INDUSIND BANK LTD, OPERA HOUSE',101,now(),now()),
('102ef08c-702a-41f7-b001-1c1df9714a5e','609000715480','RBL BANK LTD, LOWER PAREL',101,now(),now()),
('22965db7-c92c-4fb4-9343-b5d2f0b69509','409000876343','RBL BANK LTD',101,now(),now());