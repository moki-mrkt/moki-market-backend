package ua.moki.modules.products.dtos.yml_catalog;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "yml_catalog")
public record YmlCatalog(
        @JacksonXmlProperty(isAttribute = true) String date,
        Shop shop
) {}
