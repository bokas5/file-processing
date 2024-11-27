package org.task2.repository;

import org.task2.model.MatchDataDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface MatchDataRepository {
    void insertMatchData(List<MatchDataDTO> matchDataList) throws Exception;

    Map<String, LocalDateTime> getTimestamps(String runId);
}
