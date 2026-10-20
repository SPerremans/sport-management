package myfirstmodule;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** Renders an invoice snapshot as a PDF file document that only its generating user can read. */
public final class InvoicePdfRenderer {
    private static final String PDF = "MyFirstModule.InvoicePdf";
    private static final String PDF_INVOICE = "MyFirstModule.InvoicePdf_Invoice";
    private static final float MARGIN = 50, WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;
    private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private InvoicePdfRenderer() {}

    /** Replace this user's earlier PDF for the invoice, so repeated views do not accumulate files. */
    public static IMendixObject generate(IContext ctx, IMendixObject invoice) throws Exception {
        if (invoice == null) throw new IllegalArgumentException("No invoice to render.");
        long invoiceId = invoice.getId().toLong();
        List<IMendixObject> previous = Core.createXPathQuery("//" + PDF + "[" + PDF_INVOICE + "=" + invoiceId + "][System.owner='[%CurrentUser%]']").execute(ctx);
        if (!previous.isEmpty()) Core.delete(ctx, previous);
        List<IMendixObject> lines = Core.createXPathQuery("//MyFirstModule.InvoiceLine[MyFirstModule.InvoiceLine_Invoice=" + invoiceId + "]").execute(ctx);
        byte[] bytes = render(ctx, invoice, lines);
        IMendixObject pdf = Core.instantiate(ctx, PDF);
        pdf.setValue(ctx, PDF_INVOICE, invoice.getId());
        String number = text(ctx, invoice, "Number");
        String fileName = "Invoice-" + (number.isEmpty() ? String.valueOf(invoiceId) : number.replaceAll("[^A-Za-z0-9._-]", "_")) + ".pdf";
        Core.storeFileDocumentContent(ctx, pdf, fileName, new ByteArrayInputStream(bytes));
        return pdf;
    }

    private static byte[] render(IContext ctx, IMendixObject invoice, List<IMendixObject> lines) throws Exception {
        String currency = text(ctx, invoice, "Currency");
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Writer w = new Writer(document);
            w.paragraph(BOLD, 18, orDefault(text(ctx, invoice, "IssuerName"), "Invoice"));
            w.paragraph(REGULAR, 9, text(ctx, invoice, "IssuerAddress"));
            w.paragraph(REGULAR, 9, join("  |  ", label("VAT", text(ctx, invoice, "TaxNumber")), text(ctx, invoice, "ContactEmail")));
            w.gap(10);
            w.paragraph(REGULAR, 10, text(ctx, invoice, "HeaderText"));
            w.paragraph(BOLD, 14, "Invoice " + text(ctx, invoice, "Number"));
            w.paragraph(REGULAR, 10, join("    ", label("Issued", date(invoice.getValue(ctx, "IssuedAt"))),
                label("Due", date(invoice.getValue(ctx, "DueDate"))), label("Status", text(ctx, invoice, "Status"))));
            w.gap(6);
            w.paragraph(BOLD, 10, "Bill to");
            w.paragraph(REGULAR, 10, text(ctx, invoice, "RecipientName"));
            w.paragraph(REGULAR, 10, text(ctx, invoice, "RecipientEmail"));
            w.gap(6);
            w.paragraph(REGULAR, 10, text(ctx, invoice, "Description"));
            w.gap(8);
            if (!lines.isEmpty()) {
                w.row(BOLD, "Description", "Qty", "Unit price", "Amount");
                w.rule();
                for (IMendixObject line : lines) {
                    String description = join(" - ", text(ctx, line, "Description"), text(ctx, line, "MemberName"));
                    w.row(REGULAR, description, number(line.getValue(ctx, "Quantity"), 0),
                        money(line.getValue(ctx, "UnitPrice"), currency), money(line.getValue(ctx, "Amount"), currency));
                }
                w.rule();
            }
            w.row(BOLD, "Total", "", "", money(invoice.getValue(ctx, "Amount"), currency));
            w.gap(12);
            w.paragraph(REGULAR, 10, label("Payment reference", text(ctx, invoice, "PaymentReference")));
            w.paragraph(REGULAR, 10, text(ctx, invoice, "PaymentInstructions"));
            w.paragraph(REGULAR, 10, text(ctx, invoice, "BankDetails"));
            w.gap(8);
            w.paragraph(REGULAR, 9, "Payment is confirmed only after the club receives verified payment information.");
            w.paragraph(REGULAR, 9, text(ctx, invoice, "FooterText"));
            w.close();
            document.save(out);
            return out.toByteArray();
        }
    }

    /** Sequential top-to-bottom layout with wrapping and automatic page breaks. */
    private static final class Writer {
        private final PDDocument document;
        private PDPageContentStream stream;
        private float y;

        Writer(PDDocument document) throws Exception { this.document = document; newPage(); }

        private void newPage() throws Exception {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }
        private void ensure(float height) throws Exception { if (y - height < MARGIN) newPage(); }
        void gap(float height) { y -= height; }
        void paragraph(PDType1Font font, float size, String value) throws Exception {
            if (value == null || value.isBlank()) return;
            for (String part : value.split("\\r?\\n")) for (String row : wrap(font, size, safe(part), WIDTH)) {
                ensure(size * 1.4f);
                text(font, size, MARGIN, y - size, row);
                y -= size * 1.4f;
            }
        }
        void row(PDType1Font font, String description, String quantity, String unit, String amount) throws Exception {
            float size = 10, descriptionWidth = WIDTH * 0.52f;
            List<String> wrapped = wrap(font, size, safe(description), descriptionWidth);
            ensure(size * 1.4f * wrapped.size());
            for (int i = 0; i < wrapped.size(); i++) text(font, size, MARGIN, y - size - i * size * 1.4f, wrapped.get(i));
            right(font, size, MARGIN + WIDTH * 0.62f, quantity);
            right(font, size, MARGIN + WIDTH * 0.81f, unit);
            right(font, size, MARGIN + WIDTH, amount);
            y -= size * 1.4f * wrapped.size() + 2;
        }
        void rule() throws Exception {
            ensure(6);
            stream.moveTo(MARGIN, y - 2); stream.lineTo(MARGIN + WIDTH, y - 2); stream.stroke();
            y -= 6;
        }
        private void right(PDType1Font font, float size, float edge, String value) throws Exception {
            String clean = safe(value);
            text(font, size, edge - font.getStringWidth(clean) / 1000 * size, y - size, clean);
        }
        private void text(PDType1Font font, float size, float x, float baseline, String value) throws Exception {
            if (value.isEmpty()) return;
            stream.beginText(); stream.setFont(font, size); stream.newLineAtOffset(x, baseline); stream.showText(value); stream.endText();
        }
        void close() throws Exception { stream.close(); }
    }

    private static List<String> wrap(PDType1Font font, float size, String value, float width) throws Exception {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = row.length() == 0 ? word : row + " " + word;
            if (row.length() > 0 && font.getStringWidth(candidate) / 1000 * size > width) { rows.add(row.toString()); row = new StringBuilder(word); }
            else row = new StringBuilder(candidate);
        }
        rows.add(row.toString());
        return rows;
    }
    /** Standard PDF fonts only encode WinAnsi; replace anything else instead of failing the download. */
    private static String safe(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        for (char c : value.replace('\t', ' ').toCharArray()) out.append(c >= 32 && c <= 255 || c == '€' ? c : '?');
        return out.toString();
    }
    private static String text(IContext ctx, IMendixObject obj, String member) {
        return Objects.toString(obj.getValue(ctx, member), "").trim();
    }
    private static String label(String name, String value) { return value.isEmpty() ? "" : name + ": " + value; }
    private static String orDefault(String value, String fallback) { return value.isEmpty() ? fallback : value; }
    private static String join(String separator, String... values) {
        List<String> kept = new ArrayList<>();
        for (String value : values) if (value != null && !value.isEmpty()) kept.add(value);
        return String.join(separator, kept);
    }
    private static String date(Object value) { return value instanceof Date ? new SimpleDateFormat("dd/MM/yyyy").format((Date) value) : ""; }
    private static String number(Object value, int scale) {
        if (value == null) return "";
        return value instanceof BigDecimal ? ((BigDecimal) value).setScale(scale, RoundingMode.HALF_UP).toPlainString() : value.toString();
    }
    private static String money(Object value, String currency) {
        String amount = number(value, 2);
        return amount.isEmpty() ? "" : join(" ", amount, currency);
    }
}
