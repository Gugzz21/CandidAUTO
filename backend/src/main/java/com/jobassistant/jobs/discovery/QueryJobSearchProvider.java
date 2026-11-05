package com.jobassistant.jobs.discovery;

import com.jobassistant.jobs.connector.ConnectorFetchResult;

import java.util.List;

/**
 * Provedor de descoberta ativa de oportunidades a partir dos critérios/consultas do candidato,
 * retornando cada execução individual com URL consultada, código HTTP, status de extração e vagas brutas.
 */
public interface QueryJobSearchProvider {

    String providerName();

    ProviderSearchResult search(DiscoveryCriteria criteria);

    record ProviderQueryExecution(
            String connectorLabel,
            ConnectorFetchResult fetchResult
    ) {
    }

    record ProviderSearchResult(
            String providerName,
            int queriesExecuted,
            List<ProviderQueryExecution> executions,
            List<String> warnings
    ) {
    }
}
