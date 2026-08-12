package ua.moki.modules.products.dtos.yml_catalog;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public record ParamValue(
        @JacksonXmlProperty(isAttribute = true) String lang,
        @JacksonXmlText String text
) {}
