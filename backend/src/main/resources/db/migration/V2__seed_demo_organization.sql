insert into organizations (id, name, created_at)
values (
  'bc1e6b1a-8837-3056-b676-6cae794de216',
  'Demo Organization',
  current_timestamp
)
on conflict (id) do nothing;
