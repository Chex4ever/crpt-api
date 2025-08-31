package ru.selsup.trueapi.model;

public enum ProductionType {
        OWN("OWN_PRODUCTION", "Собственное производство"), CONTRACT("CONTRACT_PRODUCTION", "Производство товара по договору");

        private final String code;
        private final String description;

        ProductionType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }