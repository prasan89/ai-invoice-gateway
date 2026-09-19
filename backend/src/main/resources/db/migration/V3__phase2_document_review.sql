alter table invoices add column source_file_name varchar(255);
alter table invoices add column source_content_type varchar(100);
alter table invoices add column source_storage_path varchar(1000);
alter table invoices add column field_confidence jsonb;

update invoices
set field_confidence = '{}'::jsonb
where field_confidence is null;
