package ua.moki.modules.products.dtos.yml_catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public record SimpleParam(
        @JacksonXmlProperty(isAttribute = true) String name,

        // Додано для Prom, але якщо null - у XML не потрапить
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JacksonXmlProperty(isAttribute = true) String unit,

        @JacksonXmlText String text
) implements Param {

    // Допоміжний конструктор для Rozetka/Kasta, де unit не потрібен
    public SimpleParam(String name, String text) {
        this(name, null, text);
    }
}
