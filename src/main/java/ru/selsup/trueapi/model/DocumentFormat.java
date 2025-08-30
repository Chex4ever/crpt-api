package ru.selsup.trueapi.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DocumentFormat {
    JSON("MANUAL", "формат json"), CSV("CSV", " формат csv"), XML("XML", "формат xml");

    private final String code;
    private final String description;

    DocumentFormat(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}