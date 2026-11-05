package com.jobassistant.jobs.connector;

import com.jobassistant.jobs.domain.JobPlatform;
import com.jobassistant.jobs.domain.JobSourceEntity;

import java.util.List;

/**
 * Contrato desacoplado para conectores de descoberta de vagas em plataformas de recrutamento.
 */
public interface JobSourceConnector {

    boolean supports(JobPlatform platform);

    List<RawDiscoveredJob> fetchJobs(JobSourceEntity source);

    default ConnectorFetchResult fetchJobsSafely(JobSourceEntity source) {
        List<RawDiscoveredJob> jobs = fetchJobs(source);
        return new ConnectorFetchResult(jobs, !jobs.isEmpty(), "");
    }
}
