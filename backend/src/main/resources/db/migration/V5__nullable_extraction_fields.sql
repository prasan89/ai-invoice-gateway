alter table invoices alter column currency drop not null;
alter table invoices alter column subtotal drop not null;
alter table invoices alter column tax_amount drop not null;
alter table invoices alter column total_amount drop not null;
