package com.esprit.microservice.medicalmediaservice.services;

import com.esprit.microservice.medicalmediaservice.entities.DocumentMedical;
import com.esprit.microservice.medicalmediaservice.entities.ImageMedicale;
import com.esprit.microservice.medicalmediaservice.repositories.DocumentMedicalRepository;
import com.esprit.microservice.medicalmediaservice.repositories.ImageMedicaleRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final DocumentMedicalRepository documentRepo;
    private final ImageMedicaleRepository imageRepo;

    public byte[] exporterDossierPatient(Long patientId) throws Exception {

        List<DocumentMedical> docs = documentRepo
                .findByPatientIdOrderByDateUploadDesc(patientId);
        List<ImageMedicale> images = imageRepo
                .findByPatientIdOrderByDateExamenDesc(patientId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(50, 50, 50, 50);

        // ---- EN-TÊTE ----
        document.add(new Paragraph("DOSSIER MÉDICAL — PATIENT #" + patientId)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        document.add(new Paragraph("Généré automatiquement le : " + LocalDate.now())
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // Ligne séparatrice
        document.add(new Paragraph("─────────────────────────────────────────────────────")
                .setFontSize(10)
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // ---- RÉSUMÉ ----
        document.add(new Paragraph("Résumé du dossier")
                .setFontSize(13)
                .setBold()
                .setMarginBottom(8));

        Table resume = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginBottom(20);
        addRow(resume, "Nombre de documents", String.valueOf(docs.size()));
        addRow(resume, "Nombre d'images", String.valueOf(images.size()));
        addRow(resume, "Date d'export", LocalDate.now().toString());
        document.add(resume);

        // ---- SECTION DOCUMENTS ----
        document.add(new Paragraph("Documents médicaux (" + docs.size() + ")")
                .setFontSize(14)
                .setBold()
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(24, 95, 165))
                .setMarginBottom(10));

        if (docs.isEmpty()) {
            document.add(new Paragraph("Aucun document enregistré.")
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginBottom(15));
        } else {
            for (DocumentMedical doc : docs) {
                Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                        .useAllAvailableWidth()
                        .setMarginBottom(12);

                addRow(table, "Nom fichier", safe(doc.getNomFichier()));
                addRow(table, "Type", buildTypeLabel(doc));
                addRow(table, "Catégorie", safe(doc.getCategorie()));
                addRow(table, "Date upload", safe(doc.getDateUpload() != null ? doc.getDateUpload().toString() : null));

                if (doc.getContenuOcr() != null && !doc.getContenuOcr().isEmpty()) {
                    String ocr = doc.getContenuOcr().length() > 400
                            ? doc.getContenuOcr().substring(0, 400) + "..."
                            : doc.getContenuOcr();
                    addRow(table, "Texte OCR extrait", ocr);
                }

                document.add(table);
            }
        }

        // ---- SECTION IMAGERIE ----
        document.add(new Paragraph("Imagerie médicale (" + images.size() + ")")
                .setFontSize(14)
                .setBold()
                .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(24, 95, 165))
                .setMarginTop(10)
                .setMarginBottom(10));

        if (images.isEmpty()) {
            document.add(new Paragraph("Aucune image enregistrée.")
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginBottom(15));
        } else {
            Table tableImages = new Table(
                    UnitValue.createPercentArray(new float[]{25, 25, 50}))
                    .useAllAvailableWidth()
                    .setMarginBottom(15);

            // En-têtes
            tableImages.addHeaderCell(headerCell("Type"));
            tableImages.addHeaderCell(headerCell("Date examen"));
            tableImages.addHeaderCell(headerCell("Description"));

            for (ImageMedicale img : images) {
                tableImages.addCell(new Cell().add(new Paragraph(
                        safe(img.getTypeImagerie())).setFontSize(10)));
                tableImages.addCell(new Cell().add(new Paragraph(
                        safe(img.getDateExamen() != null ? img.getDateExamen().toString() : null))
                        .setFontSize(10)));
                tableImages.addCell(new Cell().add(new Paragraph(
                        safe(img.getDescription())).setFontSize(10)));
            }
            document.add(tableImages);
        }

        // ---- PIED DE PAGE ----
        document.add(new Paragraph(
                "\n\nDocument confidentiel — Usage médical uniquement\n" +
                        "Généré par Medical Media Service")
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        document.close();
        return baos.toByteArray();
    }

    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell().add(
                new Paragraph(label).setBold().setFontSize(10)
                        .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(60, 60, 60))));
        table.addCell(new Cell().add(
                new Paragraph(value != null ? value : "—").setFontSize(10)));
    }

    private Cell headerCell(String text) {
        return new Cell().add(new Paragraph(text)
                        .setBold().setFontSize(10)
                        .setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(24, 95, 165));
    }

    private String safe(String value) {
        return (value != null && !value.isEmpty()) ? value : "—";
    }

    private String buildTypeLabel(DocumentMedical doc) {
        StringBuilder sb = new StringBuilder(safe(doc.getType()));
        if (doc.getNiveauConfiance() != null) {
            sb.append(" — ").append(doc.getNiveauConfiance());
            if (doc.getScoreConfiance() != null) {
                sb.append(" (").append(doc.getScoreConfiance()).append("%)");
            }
        }
        return sb.toString();
    }
}