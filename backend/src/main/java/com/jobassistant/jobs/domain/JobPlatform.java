package com.jobassistant.jobs.domain;

public enum JobPlatform {
    GREENHOUSE(true, true),
    LEVER(true, true),
    GUPY(true, true),
    CAREER_PAGE(true, false),
    REMOTIVE(false, true),
    ARBEITNOW(false, true),
    LINKEDIN(false, false),
    INDEED(false, false),
    GLASSDOOR(false, false),
    PROGRAMATHOR(false, false),
    REVELO(false, false),
    INFOJOBS(false, false),
    MANUAL_URL(false, false);

    private final boolean automatedBoardSyncSupported;
    private final boolean publicQueryDiscoverySupported;

    JobPlatform(boolean automatedBoardSyncSupported, boolean publicQueryDiscoverySupported) {
        this.automatedBoardSyncSupported = automatedBoardSyncSupported;
        this.publicQueryDiscoverySupported = publicQueryDiscoverySupported;
    }

    public boolean supportsAutomatedBoardSync() {
        return automatedBoardSyncSupported;
    }

    public boolean supportsPublicQueryDiscovery() {
        return publicQueryDiscoverySupported;
    }
}
