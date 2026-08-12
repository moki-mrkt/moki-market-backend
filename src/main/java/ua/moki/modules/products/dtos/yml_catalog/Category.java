package ua.moki.modules.products.dtos.yml_catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public record Category(
        @JacksonXmlProperty(isAttribute = true) String id,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JacksonXmlProperty(isAttribute = true, localName = "rz_id")
        String rzId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JacksonXmlProperty(isAttribute = true, localName = "portal_id")
        String portalId,

        @JacksonXmlText String name
) {}
