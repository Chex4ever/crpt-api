package ru.selsup.crypto;

import java.util.List;

public interface CertificateSelector {
    CertificateInfo selectCertificate(List<CertificateInfo> certificates);

    void showMessage(String message);

    void showError(String error);
}