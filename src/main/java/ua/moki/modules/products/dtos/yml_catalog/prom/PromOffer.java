package ua.moki.modules.products.dtos.yml_catalog.prom;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.math.BigDecimal;
import java.util.List;

public record PromOffer(
        @JacksonXmlProperty(isAttribute = true) String id,
        @JacksonXmlProperty(isAttribute = true) String available,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JacksonXmlProperty(isAttribute = true, localName = "in_stock")
        String inStock,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JacksonXmlProperty(isAttribute = true, localName = "selling_type")
        String sellingType,

        String name,
        @JacksonXmlProperty(localName = "name_ua") String nameUa,

        String categoryId,
        BigDecimal price,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal oldprice,

        String currencyId,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "picture")
        List<String> pictures,

        String vendor,
        String article,

        @JacksonXmlCData
        String description,

        @JacksonXmlCData
        @JacksonXmlProperty(localName = "description_ua")
        String descriptionUa,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "param")
        List<PromParam> params
) {}
