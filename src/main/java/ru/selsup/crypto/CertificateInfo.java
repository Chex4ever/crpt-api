package ru.selsup.crypto;

import java.util.Date;

public class CertificateInfo {
    private final String alias;
    private final String subject;
    private final String issuer;
    private final Date validFrom;
    private final Date validTo;
    private final String storeType;

    public CertificateInfo(String alias, String subject, String issuer,
                           Date validFrom, Date validTo, String storeType) {
        this.alias = alias;
        this.subject = subject;
        this.issuer = issuer;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.storeType = storeType;
    }

    // Getters
    public String getAlias() { return alias; }
    public String getSubject() { return subject; }
    public String getIssuer() { return issuer; }
    public Date getValidFrom() { return validFrom; }
    public Date getValidTo() { return validTo; }
    public String getStoreType() { return storeType; }
    
    @Override
    public String toString() {
        return String.format("%s (до %s)",
                subject.length() > 50 ? subject.substring(0, 50) + "..." : subject,
                validTo.toString().substring(0, 10)
        );
    }
}