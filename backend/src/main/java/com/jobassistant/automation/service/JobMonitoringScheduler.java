package com.jobassistant.automation.service;

import com.jobassistant.automation.domain.SearchConfigEntity;
import com.jobassistant.automation.domain.SearchConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Agendador periódico de monitoramento automático de vagas.
 * Verifica a cada ciclo quais usuários possuem automação ativa e cujo intervalo individual
 * (intervalMinutes) já foi atingido desde lastRunAt, executando o monitoramento com isolamento por userId.
 */
@Component
public class JobMonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobMonitoringScheduler.class);

    private final SearchConfigRepository searchConfigRepository;
    private final AutomatedJobMonitoringService monitoringService;

    public JobMonitoringScheduler(
            SearchConfigRepository searchConfigRepository,
            AutomatedJobMonitoringService monitoringService
    ) {
        this.searchConfigRepository = searchConfigRepository;
        this.monitoringService = monitoringService;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduling.poll-ms:60000}",
            initialDelayString = "${app.scheduling.initial-delay-ms:15000}"
    )
    public void scheduledTick() {
        checkAndRunDueMonitoringCycles(Instant.now());
    }

    public int checkAndRunDueMonitoringCycles(Instant referenceNow) {
        List<SearchConfigEntity> enabledConfigs = searchConfigRepository.findAllByAutoMonitoringEnabledTrue();
        int executedUsers = 0;

        for (SearchConfigEntity config : enabledConfigs) {
            if (!isDue(config, referenceNow)) {
                continue;
            }
            try {
                log.info("Executando ciclo periódico de descoberta automática para o usuário {}", config.getUserId());
                monitoringService.runMonitoringCycleForUser(config.getUserId());
                executedUsers++;
            } catch (RuntimeException ex) {
                log.error("Falha no ciclo automático do usuário {}: {}", config.getUserId(), ex.getMessage());
                recordFailureSafely(config, referenceNow, ex.getMessage());
            }
        }

        return executedUsers;
    }

    public boolean isDue(SearchConfigEntity config, Instant now) {
        if (!config.isAutoMonitoringEnabled()) {
            return false;
        }
        Instant lastRun = config.getLastRunAt();
        if (lastRun == null) {
            return true;
        }
        long elapsedMinutes = Duration.between(lastRun, now).toMinutes();
        return elapsedMinutes >= Math.max(1, config.getIntervalMinutes());
    }

    private void recordFailureSafely(SearchConfigEntity config, Instant now, String errorMessage) {
        try {
            config.setLastRunAt(now);
            config.setLastRunSummary("Falha na execução agendada: " + (errorMessage != null ? errorMessage : "erro inesperado"));
            searchConfigRepository.save(config);
        } catch (RuntimeException ignored) {
        }
    }
}
