package com.submeter.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.submeter.api.dto.CursorPageResponse;
import com.submeter.api.dto.InvoiceResponse;
import com.submeter.entity.Invoice;
import com.submeter.entity.InvoiceLineItem;
import com.submeter.repository.InvoiceRepository;
import com.submeter.util.CursorUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepo;

    @Transactional(readOnly = true)
    public CursorPageResponse<InvoiceResponse> listInvoices(
            UUID orgId, String cursor, int limit,
            UUID customerId, UUID subscriptionId) {

        int fetchSize = Math.min(limit, 100) + 1;
        PageRequest pageRequest = PageRequest.of(0, fetchSize);

        List<Invoice> invoices;

        // Filtered queries — cursor-less for simplicity on small filtered sets
        if (customerId != null) {
            invoices = invoiceRepo.findByCustomerId(orgId, customerId, pageRequest);
        } else if (subscriptionId != null) {
            invoices = invoiceRepo.findBySubscriptionId(orgId, subscriptionId, pageRequest);
        } else if (cursor == null || cursor.isBlank()) {
            invoices = invoiceRepo.findFirstPage(orgId, pageRequest);
        } else {
            CursorUtil.Cursor parsed = CursorUtil.parse(cursor);
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid cursor");
            }
            invoices = invoiceRepo.findNextPage(orgId, parsed.createdAt(), parsed.id(), pageRequest);
        }

        boolean hasNext = invoices.size() > limit;
        if (hasNext) {
            invoices = invoices.subList(0, limit);
        }

        String nextCursor = null;
        if (hasNext && !invoices.isEmpty()) {
            Invoice last = invoices.get(invoices.size() - 1);
            nextCursor = CursorUtil.encode(last.getCreatedAt(), last.getId());
        }

        List<InvoiceResponse> dtos = invoices.stream()
                .map(InvoiceResponse::fromEntity)
                .collect(Collectors.toList());

        return new CursorPageResponse<>(dtos, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID orgId, UUID invoiceId) {
        Invoice invoice = invoiceRepo.findByIdAndOrganizationIdWithLineItems(invoiceId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        return InvoiceResponse.fromEntity(invoice);
    }

    /**
     * Generates a PDF byte array for the invoice using OpenPDF (LGPL).
     */
    @Transactional(readOnly = true)
    public byte[] generatePdf(UUID orgId, UUID invoiceId) {
        Invoice invoice = invoiceRepo.findByIdAndOrganizationIdWithLineItems(invoiceId, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            // Fonts
            Font headerFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
            Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);

            // Header
            Paragraph header = new Paragraph("INVOICE " + invoice.getInvoiceNumber(), headerFont);
            header.setAlignment(Element.ALIGN_CENTER);
            header.setSpacingAfter(20);
            document.add(header);

            // Meta info
            document.add(new Paragraph("Status: " + invoice.getStatus(), boldFont));
            document.add(new Paragraph("Period: " + invoice.getPeriodStart() + " to " + invoice.getPeriodEnd(), normalFont));
            document.add(new Paragraph("Due Date: " + invoice.getDueAt(), normalFont));
            document.add(new Paragraph("Customer: " + invoice.getSubscription().getCustomer().getName() + " (" + invoice.getSubscription().getCustomer().getEmail() + ")", normalFont));
            
            Paragraph pSpacing = new Paragraph(" ");
            pSpacing.setSpacingAfter(20);
            document.add(pSpacing);

            // Table
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 1f, 1f});

            addTableHeader(table, boldFont);

            for (InvoiceLineItem item : invoice.getLineItems()) {
                table.addCell(new Phrase(item.getDescription(), normalFont));
                table.addCell(new Phrase(String.valueOf(item.getQuantity()), normalFont));
                
                String amountStr = formatPaisa(item.getAmount());
                PdfPCell amountCell = new PdfPCell(new Phrase(amountStr, normalFont));
                amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(amountCell);
            }

            // Totals
            table.addCell(new Phrase("Subtotal", boldFont));
            table.addCell(new Phrase(""));
            PdfPCell subCell = new PdfPCell(new Phrase(formatPaisa(invoice.getSubtotalCents()), boldFont));
            subCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(subCell);

            table.addCell(new Phrase("Tax", boldFont));
            table.addCell(new Phrase(""));
            PdfPCell taxCell = new PdfPCell(new Phrase(formatPaisa(invoice.getTaxCents()), boldFont));
            taxCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(taxCell);

            table.addCell(new Phrase("Total", boldFont));
            table.addCell(new Phrase(""));
            PdfPCell totalCell = new PdfPCell(new Phrase(formatPaisa(invoice.getTotalCents()), boldFont));
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalCell);

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice {}", invoiceId, e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void addTableHeader(PdfPTable table, Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase("Description", font));
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase("Quantity", font));
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase("Amount (INR)", font));
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c3);
    }

    private String formatPaisa(long paisa) {
        return "Rs " + new BigDecimal(paisa).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP).toString();
    }
}
