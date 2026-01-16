-- Schema reference for hiring task
-- partner master
create table if not exists partner (
  id bigint auto_increment primary key,
  code varchar(64) not null unique,
  name varchar(255) not null,
  active boolean not null default true
);

-- partner fee policy
create table if not exists partner_fee_policy (
  id bigint auto_increment primary key,
  partner_id bigint not null,
  effective_from timestamp not null,
  percentage decimal(10,6) not null,
  fixed_fee decimal(15,0) null
);

create index if not exists idx_fee_partner_from on partner_fee_policy (partner_id, effective_from desc);

-- payment history
create table if not exists payment (
  id bigint auto_increment primary key,
  partner_id bigint not null,
  amount decimal(15,0) not null,
  applied_fee_rate decimal(10,6) not null,
  fee_amount decimal(15,0) not null,
  net_amount decimal(15,0) not null,
  card_bin varchar(8) null,
  card_last4 varchar(4) null,
  approval_code varchar(32) not null,
  approved_at timestamp not null,
  status varchar(20) not null,
  created_at timestamp not null,
  updated_at timestamp not null
);

create index if not exists idx_payment_created on payment (created_at desc, id desc);
create index if not exists idx_payment_partner_created on payment (partner_id, created_at desc);

-- payment gateway master
create table if not exists payment_gateway (
  id bigint auto_increment primary key,
  code varchar(32) not null unique,
  name varchar(255) not null,
  priority int not null default 0,
  active boolean not null default true
);

create index if not exists idx_active_priority on payment_gateway (active, priority);

-- partner pg support
create table if not exists partner_pg_support (
  id bigint auto_increment primary key,
  partner_id bigint not null,
  payment_gateway_id bigint not null,
  constraint uk_partner_pg unique (partner_id, payment_gateway_id),
  foreign key (partner_id) references partner(id),
  foreign key (payment_gateway_id) references payment_gateway(id)
);

create index if not exists idx_partner_id on partner_pg_support (partner_id);
