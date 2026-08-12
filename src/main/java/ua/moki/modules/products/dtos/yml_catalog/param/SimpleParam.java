package ua.moki.modules.products.dtos.yml_catalog.param;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public record SimpleParam(
        @JacksonXmlProperty(isAttribute = true) String name,
        @JacksonXmlText String text
) implements Param {}