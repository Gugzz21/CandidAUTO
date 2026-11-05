package com.jobassistant.jobs.dto;

import com.jobassistant.jobs.connector.SyncOutcomeStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Telemetria diagnóstica reproduzível de cada consulta/sincronização executada pelo motor de descoberta.
 * Responde objetivamente: URL consultada, código HTTP, presença de vagas, total extraído pelo parser,
 * total descartado com motivos detalhados, e total inserido/atualizado no banco.
 */
public record ConnectorDiagnosticDto(
        String connectorName,
        String queriedUrl,
        Integer httpStatus,
        SyncOutcomeStatus outcomeStatus,
        boolean responseContainedJobs,
        int parsedCount,
        int discardedCount,
        Map<String, Integer> discardReasons,
        int savedNewCount,
        int updatedExistingCount,
        String errorMessage,
        Instant executedAt
) {
}
