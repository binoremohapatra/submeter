while ($true) {
    docker exec submeter-postgres psql -U submeter -d submeter -c "INSERT INTO invoice_line_items (invoice_id, description, quantity, unit_amount, amount, pricing_model) SELECT id, 'Subscription Plan Charge', 1, total_cents, total_cents, 'FLAT' FROM invoices WHERE NOT EXISTS (SELECT 1 FROM invoice_line_items WHERE invoice_id = invoices.id) AND total_cents > 0;" | Out-Null
    Start-Sleep -Seconds 5
}
