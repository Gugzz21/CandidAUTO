package com.jobassistant.jobs.discovery;

import com.jobassistant.jobs.connector.ConnectorFetchResult;
import com.jobassistant.jobs.connector.GupyJobConnector;
import com.jobassistant.jobs.connector.SyncOutcomeStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pesquisa automática de vagas no Brasil consultando a API pública de busca do Portal Gupy
 * para cada cargo configurado pelo usuário, registrando telemetria completa por consulta.
 */
@Component
public class GupyPublicSearchProvider implements QueryJobSearchProvider {

    private final GupyJobConnector gupyConnector;
    private final JobRelevanceFilter relevanceFilter;

    public GupyPublicSearchProvider(
            GupyJobConnector gupyConnector,
            JobRelevanceFilter relevanceFilter
    ) {
        this.gupyConnector = gupyConnector;
        this.relevanceFilter = relevanceFilter;
    }

    @Override
    public String providerName() {
        return "Gupy Portal Público";
    }

    @Override
    public ProviderSearchResult search(DiscoveryCriteria criteria) {
        List<String> queries = relevanceFilter.buildPortugueseQueries(criteria);
        List<ProviderQueryExecution> executions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (String query : queries) {
            ConnectorFetchResult fetchResult = gupyConnector.searchPortalJobsWithDiagnostics(query, 25);
            executions.add(new ProviderQueryExecution("Gupy ('" + query + "')", fetchResult));
            if (fetchResult.outcomeStatus() == SyncOutcomeStatus.HTTP_ERROR
                    || fetchResult.outcomeStatus() == SyncOutcomeStatus.NETWORK_ERROR
                    || fetchResult.outcomeStatus() == SyncOutcomeStatus.PARSE_ERROR) {
                warnings.add("Gupy ('" + query + "'): " + fetchResult.diagnosticNote());
            }
        }

        return new ProviderSearchResult(
                providerName(),
                queries.size(),
                executions,
                warnings
        );
    }
}
