package ru.selsup.trueapi.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DocumentType {
    AGGREGATION_DOCUMENT("AGGREGATION_DOCUMENT", "Формирование упаковки", DocumentFormat.JSON),
    AGGREGATION_DOCUMENT_XML("AGGREGATION_DOCUMENT_XML", "Формирование упаковки", DocumentFormat.XML),
    SETS_AGGREGATION("SETS_AGGREGATION", "Формирование набора", DocumentFormat.JSON),
    SETS_AGGREGATION_XML("SETS_AGGREGATION_XML", "Формирование набора", DocumentFormat.XML),
    DISAGGREGATION_DOCUMENT("DISAGGREGATION_DOCUMENT", "Расформирование упаковки ", DocumentFormat.JSON),
    DISAGGREGATION_DOCUMENT_XML("DISAGGREGATION_DOCUMENT_XML", "Расформирование упаковки ", DocumentFormat.XML),
    REAGGREGATION_DOCUMENT("REAGGREGATION_DOCUMENT", "Трансформация упаковки", DocumentFormat.JSON),
    LP_INTRODUCE_GOODS("LP_INTRODUCE_GOODS", "Ввод в оборот. Производство РФ", DocumentFormat.JSON),
    LP_INTRODUCE_GOODS_CSV("LP_INTRODUCE_GOODS_CSV", "Ввод в оборот. Производство РФ", DocumentFormat.CSV),
    LP_INTRODUCE_GOODS_XML("LP_INTRODUCE_GOODS_XML", "Ввод в оборот. Производство РФ", DocumentFormat.XML),
    LP_SHIP_GOODS("LP_SHIP_GOODS", "Отгрузка", DocumentFormat.JSON),
    LP_SHIP_GOODS_CSV("LP_SHIP_GOODS_CSV", "Отгрузка", DocumentFormat.CSV),
    LP_SHIP_GOODS_XML("LP_SHIP_GOODS_XML", "Отгрузка", DocumentFormat.XML),
    EAS_GTIN_CROSSBORDER_EXPORT("EAS_GTIN_CROSSBORDER_EXPORT", "Отгрузка в ЕАЭС (ОСУ)", DocumentFormat.JSON),
    EAS_GTIN_CROSSBORDER_EXPORT_CSV("EAS_GTIN_CROSSBORDER_EXPORT_CSV", "Отгрузка в ЕАЭС (ОСУ)", DocumentFormat.CSV),
    EAS_GTIN_CROSSBORDER_ACCEPTANCE("EAS_GTIN_CROSSBORDER_ACCEPTANCE", "Приёмка из ЕАЭС (ОСУ) ", DocumentFormat.JSON),
    EAS_GTIN_CROSSBORDER_ACCEPTANCE_CSV("EAS_GTIN_CROSSBORDER_ACCEPTANCE_CSV", "Приёмка из ЕАЭС (ОСУ) ", DocumentFormat.CSV),
    REPORT_REWEIGHING("REPORT_REWEIGHING", "Отчёт о перевзвешивании", DocumentFormat.CSV),
    LP_ACCEPT_GOODS("LP_ACCEPT_GOODS", "Приемка", DocumentFormat.JSON),
    LP_ACCEPT_GOODS_XML("LP_ACCEPT_GOODS_XML", "Приемка", DocumentFormat.XML),
    LK_REMARK("LK_REMARK", "Перемаркировка", DocumentFormat.JSON),
    LK_REMARK_XML("LK_REMARK_XML", "Перемаркировка", DocumentFormat.XML),
    LP_GOODS_IMPORT("LP_GOODS_IMPORT", "Ввод в оборот. Производство вне ЕАЭС", DocumentFormat.JSON),
    LP_CANCEL_SHIPMENT("LP_CANCEL_SHIPMENT", "Отмена отгрузки", DocumentFormat.JSON),
    LK_KM_CANCELLATION("LK_KM_CANCELLATION", "Списание", DocumentFormat.JSON),
    LK_CONTRACT_COMMISSIONING("LK_CONTRACT_COMMISSIONING", "Ввод в оборот. Контрактное производство", DocumentFormat.JSON),
    LK_CONTRACT_COMMISSIONING_XML("LK_CONTRACT_COMMISSIONING_XML", "Ввод в оборот. Контрактное производство", DocumentFormat.XML),
    LK_INDI_COMMISSIONING("LK_INDI_COMMISSIONING", "Ввод в оборот. Полученных от физических лиц", DocumentFormat.JSON),
    LK_INDI_COMMISSIONING_XML("LK_INDI_COMMISSIONING_XML", "Ввод в оборот. Полученных от физических лиц", DocumentFormat.XML),
    LP_RETURN("LP_RETURN", "Возврат в оборот", DocumentFormat.JSON),
    LP_RETURN_XML("LP_RETURN_XML", "Возврат в оборот", DocumentFormat.XML),
    LP_INTRODUCE_OST("LP_INTRODUCE_OST", "Ввод в оборот. Остатки", DocumentFormat.JSON),
    CROSSBORDER("CROSSBORDER", "Ввод в оборот. Трансграничная торговля", DocumentFormat.JSON),
    FURS_CROSSBORDER("FURS_CROSSBORDER", "Ввод в оборот. Трансграничная торговля («Товары из натурального меха»)", DocumentFormat.JSON),
    LK_RECEIPT("LK_RECEIPT", "Вывод из оборота", DocumentFormat.JSON),
    LK_RECEIPT_CSV("LK_RECEIPT_CSV", "Вывод из оборота", DocumentFormat.CSV),
    LK_RECEIPT_XML("LK_RECEIPT_XML", "Вывод из оборота", DocumentFormat.XML),
    LK_RECEIPT_CANCEL("LK_RECEIPT_CANCEL", "Отмена вывода из оборота", DocumentFormat.JSON),
    LP_FTS_INTRODUCE("LP_FTS_INTRODUCE", "Ввод в оборот. Импорт с ФТС", DocumentFormat.JSON),
    LP_FTS_INTRODUCE_XML("LP_FTS_INTRODUCE_XML", "Ввод в оборот. Импорт с ФТС", DocumentFormat.XML),
    ATK_AGGREGATION("ATK_AGGREGATION", "Формирование АТК", DocumentFormat.JSON),
    ATK_AGGREGATION_XML("ATK_AGGREGATION_XML", "Формирование АТК", DocumentFormat.XML),
    ATK_TRANSFORMATION("ATK_TRANSFORMATION", "Трансформация АТК", DocumentFormat.JSON),
    ATK_DISAGGREGATION("ATK_DISAGGREGATION", "Расформирование АТК", DocumentFormat.JSON),
    WRITE_OFF("WRITE_OFF", "Списание (общий документ)", DocumentFormat.JSON),
    ALCO_UTILISED("ALCO_UTILISED", "Отчёт о нанесении ФСМ", DocumentFormat.JSON),
    EAS_CROSSBORDER_EXPORT("EAS_CROSSBORDER_EXPORT", "Отгрузка в ЕАЭС", DocumentFormat.JSON),
    EAS_CROSSBORDER_EXPORT_CSV("EAS_CROSSBORDER_EXPORT_CSV", "Отгрузка в ЕАЭС", DocumentFormat.CSV),
    LK_INDIVIDUALIZATION("LK_INDIVIDUALIZATION", "Индивидуализация КиЗ («Товары из натурального меха»)", DocumentFormat.JSON),
    LK_INDIVIDUALIZATION_XML("LK_INDIVIDUALIZATION_XML", "Индивидуализация КиЗ («Товары из натурального меха»)", DocumentFormat.XML),
    FURS_FTS_INTRODUCE("FURS_FTS_INTRODUCE", "Ввод в оборот. Импорт ФТС («Товары из натурального меха»)", DocumentFormat.JSON),
    LK_GTIN_RECEIPT("LK_GTIN_RECEIPT", "Вывод из оборота (ОСУ)", DocumentFormat.JSON),
    LK_GTIN_RECEIPT_CANCEL("LK_GTIN_RECEIPT_CANCEL", "Отмена вывода из оборота (ОСУ)", DocumentFormat.JSON),
    CIS_INFORMATION_CHANGE("CIS_INFORMATION_CHANGE", "Корректировка сведений о кодах", DocumentFormat.JSON),
    CIS_NOTICE("CIS_NOTICE", "Уведомление о состоянии кодах", DocumentFormat.JSON),
    CONNECT_TAP("CONNECT_TAP", "Подключение кега к оборудованию для розлива", DocumentFormat.JSON),
    UNIVERSAL_TRANSFER_DOCUMENT("UNIVERSAL_TRANSFER_DOCUMENT", "УПД («Пиво, напитки, изготавливаемые на основе пива, слабоалкогольные напитки» и «Слабоалкогольные напитки»)", DocumentFormat.XML),
    UNIVERSAL_TRANSFER_DOCUMENT_FIX("UNIVERSAL_TRANSFER_DOCUMENT_FIX", "УПД(и) («Пиво, напитки, изготавливаемые на основе пива, слабоалкогольные напитки» и «Слабоалкогольные напитки»)", DocumentFormat.XML),
    FIXATION("FIXATION", "УПД (отгрузка продукции) («Пиво, напитки, изготавливаемые на основе пива, слабоалкогольные напитки» и «Слабоалкогольные напитки»)", DocumentFormat.XML),
    FIXATION_CANCEL("FIXATION_CANCEL", "Отмена отгрузки по УПД («Пиво, напитки, изготавливаемые на основе пива, слабоалкогольные напитки» и «Слабоалкогольные напитки»)", DocumentFormat.JSON),
    UNIVERSAL_CORRECTION_DOCUMENT("UNIVERSAL_CORRECTION_DOCUMENT", "УКД («Пиво, напитки, изготавливаемые на основе пива, слабоалкогольные напитки»)", DocumentFormat.XML),
    UNIVERSAL_CORRECTION_DOCUMENT_FIX("UNIVERSAL_CORRECTION_DOCUMENT", "УКД(и) («Пиво, напитки, изготавливаемые на основе пива, слабоалкогольные напитки»)", DocumentFormat.XML);

    private final String code;
    private final String description;
    private final DocumentFormat format;

    DocumentType(String code, String description, DocumentFormat format) {
        this.code = code;
        this.description = description;
        this.format = format;
    }
//    public String getValue() {
//        return value;
//    }
    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public DocumentFormat getFormat() {
        return format;
    }
}