package ua.moki.modules.products.dtos.yml_catalog.prom;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "yml_catalog")
public record PromYmlCatalog(
        @JacksonXmlProperty(isAttribute = true) String date,
        PromShop shop
) {}