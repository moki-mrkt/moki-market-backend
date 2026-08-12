package ua.moki.modules.products.dtos.yml_catalog.param;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import ua.moki.modules.products.dtos.yml_catalog.ParamValue;

import java.util.List;

public record ComplexParam(
        @JacksonXmlProperty(isAttribute = true) String name,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JacksonXmlProperty(isAttribute = true) String paramid,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JacksonXmlProperty(isAttribute = true) String valueid,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "value")
        List<ParamValue> values
) implements Param {}
