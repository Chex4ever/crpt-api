package ru.selsup.trueapi.model;

public enum Organizations {
        OOO_LP("6669008900", "lp", "Предметы одежды, бельё постельное, столовое, туалетное и кухонное"),
        OOO_SHOES("6601003200", "shoes", "Обувные товары"),
        OOO_TOBACCO("2234123123", "tobacco", "Табачная продукция"),
        OOO_PERFUMERY("4455567234", "perfumery", "Духи и туалетная вода"),
        OOO_TIRES("5641249876", "tires", "Шины и покрышки пневматические резиновые новые"),
        OOO_ELECTRONICS("6100900800", "electronics", "Фотокамеры (кроме кинокамер), фотовспышки и лампы-вспышки");

        private final String inn;
        private final String name;
        private final String description;

        Organizations(String inn, String code, String description) {
            this.inn = inn;
            this.name = code;
            this.description = description;
        }

        public String getInn() {
            return inn;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }