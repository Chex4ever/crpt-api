package ru.selsup.trueapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Product {
    @JsonProperty("uit_code")
    private String uitCode;
    @JsonProperty("uitu_code")
    private String uituCode;
    @JsonProperty("tnved_code")
    private String tnvedCode;
    @JsonProperty("certificate_document_data")
    private List<CertificateDocument> certificates;


    public Product(String uitCode, String uituCode, String tnvedCode, List<CertificateDocument> certificates) {
        this.uitCode = uitCode;
        this.uituCode = uituCode;
        this.tnvedCode = tnvedCode;
        this.certificates = certificates;
    }

    public String getUitCode() { return uitCode; }

    public void setUitCode(String uitCode) {
        this.uitCode = uitCode;
    }

    public String getUituCode() {
        return uituCode;
    }

    public void setUituCode(String uituCode) {
        this.uituCode = uituCode;
    }

    public String getTnvedCode() {
        return tnvedCode;
    }

    public void setTnvedCode(String tnvedCode) {
        this.tnvedCode = tnvedCode;
    }

    public List<CertificateDocument> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<CertificateDocument> certificates) {
        this.certificates = certificates;
    }

}
