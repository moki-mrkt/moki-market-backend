package ua.moki.modules.products.dtos.yml_catalog.prom;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import ua.moki.modules.products.dtos.yml_catalog.Category;
import ua.moki.modules.products.dtos.yml_catalog.Currency;

import java.util.List;

public record PromShop(
        String name,
        String company,
        String url,

        // Використовуємо спільні DTO
        @JacksonXmlElementWrapper(localName = "currencies")
        @JacksonXmlProperty(localName = "currency")
        List<Currency> currencies,

        @JacksonXmlElementWrapper(localName = "categories")
        @JacksonXmlProperty(localName = "category")
        List<Category> categories,

        // Використовуємо специфічні DTO для Prom
        @JacksonXmlElementWrapper(localName = "offers")
        @JacksonXmlProperty(localName = "offer")
        List<PromOffer> offers
) {}
