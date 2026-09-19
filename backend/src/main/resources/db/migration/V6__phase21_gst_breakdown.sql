-- Phase 2.1: GST tax breakdown on invoice header
alter table invoices add column cgst_amount  numeric(19,2);
alter table invoices add column sgst_amount  numeric(19,2);
alter table invoices add column igst_amount  numeric(19,2);
alter table invoices add column cess_amount  numeric(19,2);

-- GST breakdown on line items
alter table invoice_lines add column hsn_sac      varchar(20);
alter table invoice_lines add column taxable_value numeric(19,2);
alter table invoice_lines add column cgst_rate    numeric(7,4);
alter table invoice_lines add column cgst_amount  numeric(19,2);
alter table invoice_lines add column sgst_rate    numeric(7,4);
alter table invoice_lines add column sgst_amount  numeric(19,2);
alter table invoice_lines add column igst_rate    numeric(7,4);
alter table invoice_lines add column igst_amount  numeric(19,2);
alter table invoice_lines add column cess_rate    numeric(7,4);
alter table invoice_lines add column cess_amount  numeric(19,2);
