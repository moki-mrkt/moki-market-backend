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
        @JacksonXmlProperty(isAttribute = true)
        String available, // String ("true"/"false") замість boolean для гнучкості

        // Специфічні атрибути для Prom
        @JacksonXmlProperty(isAttribute = true, localName = "in_stock")
        String inStock,
        @JacksonXmlProperty(isAttribute = true, localName = "selling_type")
        String sellingType,

        BigDecimal price,
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

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "param")
        List<Param> params
) {}