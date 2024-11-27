package org.task2.jpa;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_data", indexes = {
        @Index(name = "idx_match_id", columnList = "match_id"),
        @Index(name = "idx_date_insert", columnList = "date_insert"),
        @Index(name = "idx_run_id", columnList = "run_id")
})
public class MatchDataJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private String matchId;

    @Column(name = "market_id", nullable = false)
    private int marketId;

    @Column(name = "outcome_id", nullable = false)
    private String outcomeId;

    @Column(name = "specifiers")
    private String specifiers;

    @Column(name = "date_insert", nullable = false)
    private LocalDateTime dateInsert;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "event_type", nullable = false, length = 1)
    private String eventType;


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

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

}
