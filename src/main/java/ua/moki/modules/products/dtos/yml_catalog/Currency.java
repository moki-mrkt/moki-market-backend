package ua.moki.modules.products.dtos.yml_catalog;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record Currency(
        @JacksonXmlProperty(isAttribute = true) String id,
        @JacksonXmlProperty(isAttribute = true) String rate
) {}