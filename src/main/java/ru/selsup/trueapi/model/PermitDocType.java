package ru.selsup.trueapi.model;

public enum PermitDocType {
    CONFORMITY_CERTIFICATE(1, "CONFORMITY_CERTIFICATE", "Сертификат соответствия"),
    CONFORMITY_DECLARATION(2, "CONFORMITY_DECLARATION", "Декларация о соответствии"),
    STATE_REGISTRATION_CERTIFICATE(4, "STATE_REGISTRATION_CERTIFICATE", "Свидетельство о государственной регистрации"),
    REGISTRATION_VET_CERTIFICATE(6, "REGISTRATION_VET_CERTIFICATE", "Регистрационное удостоверение лекарственного препарата для\n" +
            "ветеринарного применения");

    private final int id;
    private final String name;
    private final String description;

    PermitDocType(int id, String name, String description) {
        this.id=id;
        this.name=name;
        this.description=description;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
