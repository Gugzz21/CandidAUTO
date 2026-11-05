package com.jobassistant.jobs.connector;

import java.util.List;

/**
 * Resultado enriquecido de sincronização de uma fonte de vagas.
 * Permite distinguir um quadro comprovadamente vazio (authoritativeEmpty = true / SUCCESS_EMPTY_CONFIRMED)
 * de uma extração inconclusiva/bloqueada/HTML alterado/erro de parse, evitando expirar vagas ativas indevidamente.
 */
public record ConnectorFetchResult(
        List<RawDiscoveredJob> jobs,
        boolean authoritativeEmpty,
        SyncOutcomeStatus outcomeStatus,
        String queriedUrl,
        Integer httpStatus,
        String diagnosticNote
) {
    public ConnectorFetchResult {
        jobs = jobs != null ? jobs : List.of();
        outcomeStatus = outcomeStatus != null
                ? outcomeStatus
                : (!jobs.isEmpty() ? SyncOutcomeStatus.SUCCESS_WITH_RESULTS
                : (authoritativeEmpty ? SyncOutcomeStatus.SUCCESS_EMPTY_CONFIRMED : SyncOutcomeStatus.EXTRACTION_INCONCLUSIVE));
        queriedUrl = queriedUrl != null ? queriedUrl : "";
        diagnosticNote = diagnosticNote != null ? diagnosticNote : "";
    }

    public ConnectorFetchResult(List<RawDiscoveredJob> jobs, boolean authoritativeEmpty, String diagnosticNote) {
        this(
                jobs,
                authoritativeEmpty,
                !jobs.isEmpty()
                        ? SyncOutcomeStatus.SUCCESS_WITH_RESULTS
                        : (authoritativeEmpty ? SyncOutcomeStatus.SUCCESS_EMPTY_CONFIRMED : SyncOutcomeStatus.EXTRACTION_INCONCLUSIVE),
                "",
                200,
                diagnosticNote
        );
    }
}
