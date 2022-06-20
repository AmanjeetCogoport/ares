create table payment_sequence_numbers
(
  id serial,
  sequence_type varchar(10),
  next_sequence_number bigint,
  created_at timestamp not null default now(),
  updated_at timestamp not null default now(),
  primary key(id)
);

insert into payment_sequence_numbers(sequence_type,next_sequence_number,created_at,updated_at)
values('REC',2022,now(),now()),('PAY',2022,now(),now());

alter table payments add payment_num bigint,add payment_num_value varchar(25);