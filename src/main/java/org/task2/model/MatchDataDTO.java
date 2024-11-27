package org.task2.model;

import java.time.LocalDateTime;

public class MatchDataDTO {
    private String matchId;
    private int marketId;
    private String outcomeId;
    private String specifiers;
    private LocalDateTime dateInsert;
    private String runId;
    private String eventType; // 'A' or 'B'
    private int sequenceNumber;


    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public int getMarketId() {
        return marketId;
    }

    public void setMarketId(int marketId) {
        this.marketId = marketId;
    }

    public String getOutcomeId() {
        return outcomeId;
    }

    public void setOutcomeId(String outcomeId) {
        this.outcomeId = outcomeId;
    }

    public String getSpecifiers() {
        return specifiers;
    }

    public void setSpecifiers(String specifiers) {
        this.specifiers = specifiers;
    }

    public LocalDateTime getDateInsert() {
        return dateInsert;
    }

    public void setDateInsert(LocalDateTime dateInsert) {
        this.dateInsert = dateInsert;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public String toString() {
        return "MatchDataDTO{" +
                "matchId='" + matchId + '\'' +
                ", marketId=" + marketId +
                ", outcomeId='" + outcomeId + '\'' +
                ", specifiers='" + specifiers + '\'' +
                ", dateInsert=" + dateInsert +
                ", runId='" + runId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", sequenceNumber=" + sequenceNumber +
                '}';
    }
}
