ALTER TABLE invoice_lines
    ALTER COLUMN discount      DROP NOT NULL,
    ALTER COLUMN unit_price    DROP NOT NULL,
    ALTER COLUMN quantity      DROP NOT NULL,
    ALTER COLUMN tax_rate      DROP NOT NULL,
    ALTER COLUMN tax_amount    DROP NOT NULL,
    ALTER COLUMN taxable_value DROP NOT NULL;
