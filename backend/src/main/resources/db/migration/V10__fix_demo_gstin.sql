-- Fix demo GSTINs that failed checksum validation
UPDATE invoices
SET supplier_gstin        = '29AAAPL2345A1Z8',
    supplier_gstin_status = 'VALID'
WHERE supplier_gstin = '29AAAAA0000A1Z5';

UPDATE invoices
SET customer_gstin        = '33BBBPL3456B1Z7',
    customer_gstin_status = 'VALID'
WHERE customer_gstin = '33BBBBB0000B1Z6';

UPDATE vendors
SET gstin = '29AAAPL2345A1Z8'
WHERE gstin = '29AAAAA0000A1Z5';

-- Re-evaluate auto-approval: set AUTO_APPROVED for high-confidence invoices
-- that previously failed only because of invalid GSTIN
UPDATE invoices
SET status = 'AUTO_APPROVED'
WHERE status = 'REVIEW_REQUIRED'
  AND supplier_gstin_status = 'VALID'
  AND customer_gstin_status = 'VALID'
  AND arithmetic_status = 'PASS'
  AND (duplicate_score IS NULL OR duplicate_score < 80)
  AND extraction_confidence >= 95
  AND total_amount < 500000;
