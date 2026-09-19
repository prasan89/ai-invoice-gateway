create table invoice_events (
  id uuid primary key,
  invoice_id uuid not null references invoices(id) on delete cascade,
  event_type varchar(50) not null,
  message varchar(2000) not null,
  created_at timestamp with time zone not null
);

create index idx_invoice_events_invoice on invoice_events(invoice_id, created_at desc);
