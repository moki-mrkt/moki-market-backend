package ua.moki.modules.products.dtos.yml_catalog;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public record Shop(
        String name,
        String company,
        String url,

        @JacksonXmlElementWrapper(localName = "currencies")
        @JacksonXmlProperty(localName = "currency")
        List<Currency> currencies,

        @JacksonXmlElementWrapper(localName = "categories")
        @JacksonXmlProperty(localName = "category")
        List<Category> categories,

        @JacksonXmlElementWrapper(localName = "offers")
        @JacksonXmlProperty(localName = "offer")
        List<Offer> offers
) {
}
