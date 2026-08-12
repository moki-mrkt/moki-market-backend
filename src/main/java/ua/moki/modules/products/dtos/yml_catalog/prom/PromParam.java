package ua.moki.modules.products.dtos.yml_catalog.prom;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public record PromParam(
        @JacksonXmlProperty(isAttribute = true) String name,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JacksonXmlProperty(isAttribute = true) String unit,

        @JacksonXmlText String text
) {}