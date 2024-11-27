package org.task2.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.task2.model.MatchDataDTO;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MatchDataRepositoryImpl implements MatchDataRepository {

    private final DataSource dataSource;

    @Inject
    public MatchDataRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insertMatchData(List<MatchDataDTO> matchDataList) throws Exception {
        String insertSql = "INSERT INTO match_data (match_id, market_id, outcome_id, specifiers, date_insert, run_id, sequence_number, event_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

            for (MatchDataDTO dto : matchDataList) {
                pstmt.setString(1, dto.getMatchId());
                pstmt.setInt(2, dto.getMarketId());
                pstmt.setString(3, dto.getOutcomeId());
                pstmt.setString(4, dto.getSpecifiers());
                pstmt.setTimestamp(5, Timestamp.valueOf(dto.getDateInsert()));
                pstmt.setString(6, dto.getRunId());
                pstmt.setInt(7, dto.getSequenceNumber());
                pstmt.setString(8, dto.getEventType()); // Set eventType
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        } catch (SQLException e) {
            throw e; // Rethrow to allow higher layers to handle if necessary
        }
    }


    @Override
    public Map<String, LocalDateTime> getTimestamps(String runId) {
        String query = "SELECT MIN(date_insert) AS min_date, MAX(date_insert) AS max_date " +
                "FROM match_data WHERE run_id = ?";
        Map<String, LocalDateTime> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, runId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp minTimestamp = rs.getTimestamp("min_date");
                    Timestamp maxTimestamp = rs.getTimestamp("max_date");
                    if (minTimestamp != null) {
                        result.put("min_date", minTimestamp.toLocalDateTime());
                    }
                    if (maxTimestamp != null) {
                        result.put("max_date", maxTimestamp.toLocalDateTime());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Consider using a logger instead of printStackTrace
        }
        return result;
    }
}
