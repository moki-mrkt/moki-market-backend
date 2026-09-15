package ua.moki.infrastructure.storage.service;

import ua.moki.infrastructure.storage.dtos.TranslatorDTO;

public interface TranslatorService {

    TranslatorDTO translateTextsToRu(TranslatorDTO translateUaDTO);
}
