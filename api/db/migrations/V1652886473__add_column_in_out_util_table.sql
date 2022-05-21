  create type DOCUMENT_STATUS as ENUM ('final','cancelled');

  alter table account_utilizations
  add zone_code varchar(10) , add doc_status varchar(15);

  alter table account_utilizations
  add constraint chk_zone check(zone_code in('north','south','east','west'));

  alter table account_utilizations
  add constraint chk_doc_status check(doc_status in('final','proforma','cancelled'));