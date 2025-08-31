package ru.selsup.trueapi.model;

public enum ProductGroup {
        LP(1, "lp", "Предметы одежды, бельё постельное, столовое, туалетное и кухонное"),
        SHOES(2, "shoes", "Обувные товары"),
        TOBACCO(3, "tobacco", "Табачная продукция"),
        PERFUMERY(4, "perfumery", "Духи и туалетная вода"),
        TIRES(5, "tires", "Шины и покрышки пневматические резиновые новые"), ELECTRONICS(6, "electronics", "Фотокамеры (кроме кинокамер), фотовспышки и лампы-вспышки"), MILK(8, "milk", "Молочная продукция"), BICYCLE(9, "bicycle", "Велосипеды и велосипедные рамы"), WHEELCHAIRS(10, "wheelchairs", "Медицинские изделия"), ALCOHOL(11, "alcohol", "Слабоалкогольные напитки"), OTP(12, "otp", "Альтернативная табачная продукция"), WATER(13, "water", "Упакованная вода"), FURS(14, "furs", "Товары из натурального меха"), BEER(15, "beer", "Пиво, напитки, изготавливаемые на основе пива, слабоалкогольные напитки"), NCP(16, "ncp", "Никотиносодержащая продукция"), BIO(17, "bio", "Биологически активные добавки к пище"), ANTISEPTIC(19, "antiseptic", "Антисептики и дезинфицирующие средства"), PETFOOD(20, "petfood", "Корма для животных"), SEAFOOD(21, "seafood", "Морепродукты"), NABEER(22, "nabeer", "Безалкогольное пиво"), SOFTDRINKS(23, "softdrinks", "Соковая продукция и безалкогольные напитки"), VETPHARMA(26, "vetpharma", "Ветеринарные препараты"), TOYS(27, "toys", "Игры и игрушки для детей"), RADIO(28, "radio", "Радиоэлектронная продукция"), TITAN(31, "titan", "Титановая металлопродукция"), CONSERVE(32, "conserve", "Консервированная продукция"), VEGETABLEOIL(33, "vegetableoil", "Растительные масла"), OPTICFIBER(34, "opticfiber", "Оптоволокно и оптоволоконная продукция"), CHEMISTRY(35, "chemistry", "Парфюмерные и косметические средства и бытовая химия"), BOOKS(36, "books", "Печатная продукция"), GROCERY(37, "grocery", "Бакалейная продукция"), PHARMARAW(38, "pharmaraw", "Фармацевтическое сырьё, лекарственные средства"), CONSTRUCTION(39, "construction", "Строительные материалы"), FIRE(40, "fire", "Пиротехника и огнетушащее оборудование"), HEATER(41, "heater", "Отопительные приборы"), CABLERAW(42, "cableraw", "Кабельно-проводниковая продукция"), AUTOFLUIDS(43, "autofluids", "Моторные масла"), POLYMER(44, "polymer", "Полимерные трубы"), SWEETS(45, "sweets", "Сладости и кондитерские изделия"), CARPARTS(48, "carparts", "Автозапчасти и комплектующие транспортных средств");

        private final int id;
        private final String code;
        private final String description;

        ProductGroup(int id, String code, String description) {
            this.id = id;
            this.code = code;
            this.description = description;
        }

        public static ProductGroup fromCode(String code) {
            for (ProductGroup group : values()) {
                if (group.code.equals(code)) {
                    return group;
                }
            }
            throw new IllegalArgumentException("Unknown product group code: " + code);
        }

        public int getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }