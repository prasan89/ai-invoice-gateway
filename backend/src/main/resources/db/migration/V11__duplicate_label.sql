ALTER TABLE invoices ADD COLUMN IF NOT EXISTS duplicate_label VARCHAR(20);

UPDATE invoices SET duplicate_label = CASE
  WHEN duplicate_score >= 90 THEN 'CONFIRMED'
  WHEN duplicate_score >= 60 THEN 'POTENTIAL'
  ELSE NULL
END;
