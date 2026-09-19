create table organizations (
  id uuid primary key,
  name varchar(200) not null,
  created_at timestamp with time zone not null
);

create table invoices (
  id uuid primary key,
  organization_id uuid not null references organizations(id),
  invoice_number varchar(100) not null,
  invoice_date date,
  currency varchar(10) not null,
  supplier_name varchar(250),
  supplier_gstin varchar(20),
  customer_name varchar(250),
  customer_gstin varchar(20),
  subtotal numeric(19,2) not null,
  tax_amount numeric(19,2) not null,
  total_amount numeric(19,2) not null,
  extraction_confidence numeric(5,2),
  status varchar(40) not null,
  validation_message varchar(1000),
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null
);

create table invoice_lines (
  id uuid primary key,
  invoice_id uuid not null references invoices(id) on delete cascade,
  description varchar(1000) not null,
  quantity numeric(19,4) not null,
  unit_price numeric(19,2) not null,
  discount numeric(19,2) not null,
  tax_rate numeric(7,4) not null,
  tax_amount numeric(19,2) not null,
  line_total numeric(19,2) not null
);

create index idx_invoice_org on invoices(organization_id);
create index idx_invoice_status on invoices(status);
