package ru.selsup.trueapi.model;

public class CertificateDocument {
    private PermitDocType certificateDocumentType;
    private String certificateDocumentNumber;
    private String certificateDocumentDate;

    public CertificateDocument(PermitDocType certificateDocumentType, String certificateDocumentNumber, String certificateDocumentDate) {
        this.certificateDocumentType = certificateDocumentType;
        this.certificateDocumentNumber = certificateDocumentNumber;
        this.certificateDocumentDate = certificateDocumentDate;
    }

    public PermitDocType getCertificateDocumentType() {
        return certificateDocumentType;
    }

    public void setCertificateDocumentType(PermitDocType certificateDocumentType) {
        this.certificateDocumentType = certificateDocumentType;
    }

    public String getCertificateDocumentNumber() {
        return certificateDocumentNumber;
    }

    public void setCertificateDocumentNumber(String certificateDocumentNumber) {
        this.certificateDocumentNumber = certificateDocumentNumber;
    }

    public String getCertificateDocumentDate() {
        return certificateDocumentDate;
    }

    public void setCertificateDocumentDate(String certificateDocumentDate) {
        this.certificateDocumentDate = certificateDocumentDate;
    }
}