package ua.moki.modules.products.dtos.yml_catalog;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import ua.moki.modules.products.dtos.yml_catalog.param.Param;

import java.math.BigDecimal;
import java.util.List;

public record Offer(
        @JacksonXmlProperty(isAttribute = true) String id,
        @JacksonXmlProperty(isAttribute = true) boolean available,
        BigDecimal price,
        @JacksonXmlProperty(localName = "price_old") BigDecimal priceOld,
        @JacksonXmlProperty(localName = "price_promo") BigDecimal pricePromo,
        String article,
        @JacksonXmlProperty(localName = "stock_quantity") int stockQuantity,
        String url,
        String currencyId,
        String categoryId,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "picture")
        List<String> pictures,

        String vendor,
        String name,
        String description,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "param")
        List<Param> params
) {}
