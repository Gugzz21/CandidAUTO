package com.jobassistant.jobs.connector;

/**
 * Classificação explícita do resultado de consulta e parsing de um conector de vagas,
 * separando sucesso com vagas, sucesso comprovadamente vazio, extração inconclusiva,
 * falha de parsing, erro HTTP e falha de rede.
 */
public enum SyncOutcomeStatus {
    SUCCESS_WITH_RESULTS,
    SUCCESS_EMPTY_CONFIRMED,
    EXTRACTION_INCONCLUSIVE,
    PARSE_ERROR,
    HTTP_ERROR,
    NETWORK_ERROR
}
