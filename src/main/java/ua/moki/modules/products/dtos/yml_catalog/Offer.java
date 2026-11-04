package ua.moki.modules.products.dtos.yml_catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;


import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Offer(
        @JacksonXmlProperty(isAttribute = true)
        String id,
        @JacksonXmlProperty(isAttribute = true, localName = "group_id")
        String groupId,
        @JacksonXmlProperty(isAttribute = true)
        String available,
        @JacksonXmlProperty(isAttribute = true, localName = "in_stock")
        String inStock,
        @JacksonXmlProperty(isAttribute = true, localName = "selling_type")
        String sellingType,

        BigDecimal price,
        @JacksonXmlProperty(localName = "oldprice")
        BigDecimal oldPrice,
        @JacksonXmlProperty(localName = "price_old")
        BigDecimal priceOld,
        @JacksonXmlProperty(localName = "price_promo")
        BigDecimal pricePromo,

        String article,
        @JacksonXmlProperty(localName = "stock_quantity")
        Integer stockQuantity,

        String url,
        String currencyId,
        String categoryId,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "picture")
        List<String> pictures,

        String vendor,
        String name,

        // Специфічно для Prom
        @JacksonXmlProperty(localName = "name_ua")
        String nameUa,

        @JacksonXmlCData
        String description,

        // Специфічно для Prom
        @JacksonXmlCData
        @JacksonXmlProperty(localName = "description_ua")
        String descriptionUa,

        // Специфічно для Prom ("Пошукові запити")
        @JacksonXmlProperty(localName = "keywords")
        String keywords,

        @JacksonXmlProperty(localName = "keywords_ua")
        String keywordsUa,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "param")
        List<Param> params
) {
    public Offer(
            String id,
            String groupId,
            String available,
            String inStock,
            String sellingType,
            BigDecimal price,
            BigDecimal priceOld,
            BigDecimal pricePromo,
            String article,
            Integer stockQuantity,
            String url,
            String currencyId,
            String categoryId,
            List<String> pictures,
            String vendor,
            String name,
            String nameUa,
            String description,
            String descriptionUa,
            String keywords,
            String keywordsUa,
            List<Param> params
    ) {
        this(
                id, groupId, available, inStock, sellingType, price, null, priceOld, pricePromo,
                article, stockQuantity, url, currencyId, categoryId, pictures, vendor,
                name, nameUa, description, descriptionUa, keywords, keywordsUa, params
        );
    }

    public Offer(
            String id,
            String groupId,
            String available,
            String inStock,
            String sellingType,
            BigDecimal price,
            BigDecimal priceOld,
            BigDecimal pricePromo,
            String article,
            Integer stockQuantity,
            String url,
            String currencyId,
            String categoryId,
            List<String> pictures,
            String vendor,
            String name,
            String nameUa,
            String description,
            String descriptionUa,
            List<Param> params
    ) {
        this(
                id, groupId, available, inStock, sellingType, price, null, priceOld, pricePromo,
                article, stockQuantity, url, currencyId, categoryId, pictures, vendor,
                name, nameUa, description, descriptionUa, null, null, params
        );
    }
}