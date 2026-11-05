package com.jobassistant.jobs.discovery;

import com.jobassistant.jobs.connector.ConnectorFetchResult;
import com.jobassistant.jobs.connector.GreenhouseJobConnector;
import com.jobassistant.jobs.connector.LeverJobConnector;
import com.jobassistant.jobs.connector.SyncOutcomeStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Consulta automática em um catálogo curado de quadros públicos oficiais na Greenhouse e Lever,
 * registrando telemetria individual por board consultado.
 */
@Component
public class CuratedBoardsDiscoveryProvider implements QueryJobSearchProvider {

    private final GreenhouseJobConnector greenhouseConnector;
    private final LeverJobConnector leverConnector;
    private final List<BoardEntry> greenhouseBoards;
    private final List<BoardEntry> leverBoards;

    public CuratedBoardsDiscoveryProvider(
            GreenhouseJobConnector greenhouseConnector,
            LeverJobConnector leverConnector,
            @Value("${app.discovery.curated-greenhouse-boards:nubank:Nubank,brex:Brex,gitlab:GitLab,cloudflare:Cloudflare}") String ghBoardsSpec,
            @Value("${app.discovery.curated-lever-boards:}") String leverBoardsSpec
    ) {
        this.greenhouseConnector = greenhouseConnector;
        this.leverConnector = leverConnector;
        this.greenhouseBoards = parseBoardsSpec(ghBoardsSpec);
        this.leverBoards = parseBoardsSpec(leverBoardsSpec);
    }

    @Override
    public String providerName() {
        return "Catálogo Curado (Greenhouse & Lever)";
    }

    @Override
    public ProviderSearchResult search(DiscoveryCriteria criteria) {
        List<ProviderQueryExecution> executions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int queries = 0;

        for (BoardEntry board : greenhouseBoards) {
            queries++;
            String label = "Greenhouse (" + board.companyName() + ")";
            try {
                ConnectorFetchResult res = greenhouseConnector.fetchBoardByToken(board.identifier(), board.companyName());
                executions.add(new ProviderQueryExecution(label, res));
            } catch (RuntimeException ex) {
                warnings.add(label + ": " + ex.getMessage());
                executions.add(new ProviderQueryExecution(label, new ConnectorFetchResult(
                        List.of(),
                        false,
                        SyncOutcomeStatus.NETWORK_ERROR,
                        board.identifier(),
                        null,
                        ex.getMessage()
                )));
            }
        }

        for (BoardEntry board : leverBoards) {
            queries++;
            String label = "Lever (" + board.companyName() + ")";
            try {
                ConnectorFetchResult res = leverConnector.fetchBoardBySite(board.identifier(), board.companyName());
                executions.add(new ProviderQueryExecution(label, res));
            } catch (RuntimeException ex) {
                warnings.add(label + ": " + ex.getMessage());
                executions.add(new ProviderQueryExecution(label, new ConnectorFetchResult(
                        List.of(),
                        false,
                        SyncOutcomeStatus.NETWORK_ERROR,
                        board.identifier(),
                        null,
                        ex.getMessage()
                )));
            }
        }

        return new ProviderSearchResult(providerName(), queries, executions, warnings);
    }

    private List<BoardEntry> parseBoardsSpec(String spec) {
        if (spec == null || spec.isBlank()) {
            return List.of();
        }
        List<BoardEntry> list = new ArrayList<>();
        for (String item : spec.split(",")) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) continue;
            int colonIdx = trimmed.indexOf(':');
            if (colonIdx > 0) {
                String token = trimmed.substring(0, colonIdx).trim();
                String name = trimmed.substring(colonIdx + 1).trim();
                if (!token.isEmpty()) {
                    list.add(new BoardEntry(token, name.isEmpty() ? capitalize(token) : name));
                }
            } else {
                list.add(new BoardEntry(trimmed, capitalize(trimmed)));
            }
        }
        return list;
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private record BoardEntry(String identifier, String companyName) {
    }
}
