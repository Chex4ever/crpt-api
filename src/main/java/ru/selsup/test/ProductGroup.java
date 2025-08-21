package ru.selsup.test;

public enum ProductGroup {
    CLOTHES("clothes", "Предметы одежды, белье постельное, столовое, туалетное и кухонное"),
    SHOES("shoes", "Обувные товары"),
    TOBACCO("tobacco", "Табачная продукция"),
    PERFUMERY("perfumery", "Духи и туалетная вода"),
    TIRES("tires", "Шины и покрышки пневматические резиновые новые"),
    ELECTRONICS("electronics", "Фотокамеры (кроме кинокамер), фотовспышки и лампы-вспышки"),
    PHARMA("pharma", "Лекарственные препараты для медицинского применения"),
    MILK("milk", "Молочная продукция"),
    BICYCLE("bicycle", "Велосипеды и велосипедные рамы"),
    WHEELCHAIRS("wheelchairs", "Кресла-коляски");

    private final String code;
    private final String description;

    ProductGroup(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ProductGroup fromCode(String code) {
        for (ProductGroup group : values()) {
            if (group.code.equals(code)) {
                return group;
            }
        }
        throw new IllegalArgumentException("Unknown product group code: " + code);
    }
}