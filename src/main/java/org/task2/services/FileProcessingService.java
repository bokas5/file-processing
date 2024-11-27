package org.task2.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.task2.model.MatchDataDTO;
import org.task2.repository.MatchDataRepository;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class FileProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);

    private static final int BATCH_SIZE = 1000;
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static int runCounter = 0; // Static counter for runId

    @Inject
    MatchDataRepository matchDataRepository;

    // Map to track the next expected sequence number per match_id
    private final ConcurrentMap<String, MatchIdSequence> matchIdSequenceMap = new ConcurrentHashMap<>();

    // Inner class to manage sequencing per match_id
    private static class MatchIdSequence {
        private int expectedSequence;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();

        public MatchIdSequence() {
            this.expectedSequence = 1;
        }

        public void waitForTurn(int sequenceNumber) throws InterruptedException {
            lock.lock();
            try {
                while (sequenceNumber != expectedSequence) {
                    condition.await();
                }
            } finally {
                lock.unlock();
            }
        }

        public void signalNext() {
            lock.lock();
            try {
                expectedSequence++;
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Processes the data file, ensuring that records are inserted in order per MATCH_ID.
     *
     * @param fileName The name of the data file located in the classpath (e.g., src/main/resources).
     */
    public void processFileStreamUsingCopy(String fileName) {
        // Separate collections for 'A' and 'B' events
        Map<String, List<MatchDataDTO>> matchIdToAEvents = new ConcurrentHashMap<>();
        Map<String, List<MatchDataDTO>> matchIdToBEvents = new ConcurrentHashMap<>();
        int runId = getNextRunId();

        // Step 1: Read and parse the file
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // Skip header
                }

                String[] parts = line.split("\\|");
                if (parts.length < 3) {
                    logger.warn("Skipping malformed line: {}", line);
                    continue;
                }

                MatchDataDTO matchData = new MatchDataDTO();
                matchData.setMatchId(parts[0].replaceAll("'", ""));
                matchData.setMarketId(Integer.parseInt(parts[1].replaceAll("'", "")));
                matchData.setOutcomeId(parts[2].replaceAll("'", ""));
                matchData.setSpecifiers(parts.length > 3 ? parts[3].replaceAll("'", "") : null);
                matchData.setDateInsert(LocalDateTime.now());
                matchData.setRunId(String.valueOf(runId));

                // Assign Event Type based on SPECIFIERS
                char eventType = (matchData.getSpecifiers() != null && !matchData.getSpecifiers().isEmpty()) ? 'A' : 'B';
                matchData.setEventType(String.valueOf(eventType));

                // Assign Sequence Number
                int sequenceNumber = 1; // Initialize, will set later

                // Group by MATCH_ID and Event Type
                if (eventType == 'A') {
                    matchIdToAEvents.computeIfAbsent(matchData.getMatchId(), k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(matchData);
                } else {
                    matchIdToBEvents.computeIfAbsent(matchData.getMatchId(), k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(matchData);
                }
            }

        } catch (Exception e) {
            logger.error("Error reading the file: {}", e.getMessage(), e);
            return;
        }

        // Step 2: Assign Sequence Numbers within each MATCH_ID
        assignSequenceNumbers(matchIdToAEvents);
        assignSequenceNumbers(matchIdToBEvents);

        // Step 3: Process 'A' and 'B' events separately
        processAEvents(matchIdToAEvents);
        processBEvents(matchIdToBEvents);
    }

    private void assignSequenceNumbers(Map<String, List<MatchDataDTO>> matchIdToEvents) {
        for (Map.Entry<String, List<MatchDataDTO>> entry : matchIdToEvents.entrySet()) {
            List<MatchDataDTO> events = entry.getValue();
            // Sort events by dateInsert to maintain sequence
            events.sort(Comparator.comparing(MatchDataDTO::getDateInsert));
            for (int i = 0; i < events.size(); i++) {
                events.get(i).setSequenceNumber(i + 1);
            }
        }
    }

    private void insertBatch(List<MatchDataDTO> batch) throws Exception {
        matchDataRepository.insertMatchData(batch);
        logger.info("Inserted batch of size {} for run_id {}", batch.size(), batch.get(0).getRunId());
    }

    private void processAEvents(Map<String, List<MatchDataDTO>> matchIdToAEvents) {
        // Single-threaded executor or limited concurrency
        ExecutorService aExecutor = Executors.newFixedThreadPool(THREAD_COUNT / 2);

        List<Callable<Void>> aTasks = new ArrayList<>();

        for (Map.Entry<String, List<MatchDataDTO>> entry : matchIdToAEvents.entrySet()) {
            String matchId = entry.getKey();
            List<MatchDataDTO> dataList = entry.getValue();

            Callable<Void> task = () -> {
                try {
                    List<MatchDataDTO> batch = new ArrayList<>();
                    for (MatchDataDTO dto : dataList) {
                        batch.add(dto);
                        if (batch.size() >= BATCH_SIZE) {
                            insertBatch(batch);
                            batch.clear();
                        }
                    }
                    if (!batch.isEmpty()) {
                        insertBatch(batch);
                    }
                } catch (Exception e) {
                    logger.error("Error inserting A events for MATCH_ID {}: {}", matchId, e.getMessage(), e);
                }
                return null;
            };

            aTasks.add(task);
        }

        try {
            List<Future<Void>> futures = aExecutor.invokeAll(aTasks);
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error processing A events: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        } finally {
            aExecutor.shutdown();
        }
    }

    private void processBEvents(Map<String, List<MatchDataDTO>> matchIdToBEvents) {
        ExecutorService bExecutor = Executors.newFixedThreadPool(THREAD_COUNT * 2);
        List<Callable<Void>> bTasks = new ArrayList<>();

        for (Map.Entry<String, List<MatchDataDTO>> entry : matchIdToBEvents.entrySet()) {
            String matchId = entry.getKey();
            List<MatchDataDTO> dataList = entry.getValue();

            Callable<Void> task = () -> {
                try {
                    for (MatchDataDTO dto : dataList) {
                        MatchIdSequence seq = matchIdSequenceMap.get(dto.getMatchId());
                        if (seq != null) {
                            seq.waitForTurn(dto.getSequenceNumber());
                            insertBatch(Collections.singletonList(dto));
                            seq.signalNext();
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing B events for MATCH_ID {}: {}", matchId, e.getMessage(), e);
                }
                return null;
            };

            bTasks.add(task);
        }

        try {
            List<Future<Void>> futures = bExecutor.invokeAll(bTasks);
            for (Future<Void> future : futures) {
                try {
                    future.get(); // Will throw exception if the task failed
                } catch (ExecutionException ee) {
                    logger.error("Task execution failed: {}", ee.getCause().getMessage(), ee.getCause());
                }
            }
        } catch (InterruptedException ie) {
            logger.error("B Executor interrupted: {}", ie.getMessage(), ie);
            Thread.currentThread().interrupt(); // Restore interrupt status
        } finally {
            bExecutor.shutdown();
            try {
                if (!bExecutor.awaitTermination(1, TimeUnit.HOURS)) {
                    bExecutor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                logger.error("Error shutting down bExecutor: {}", ie.getMessage(), ie);
                bExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }



    /**
     * Processes a batch of MatchDataDTO records, ensuring correct ordering based on sequenceNumber.
     *
     * @param batch The list of MatchDataDTO records to process.
     * @throws Exception if processing fails.
     */
    private void processBatch(List<MatchDataDTO> batch) throws Exception {
        for (MatchDataDTO dto : batch) {
            MatchIdSequence seq = matchIdSequenceMap.get(dto.getMatchId());
            if (seq != null) {
                // Wait for the turn based on sequenceNumber
                seq.waitForTurn(dto.getSequenceNumber());

                // Write to output (database)
                matchDataRepository.insertMatchData(Collections.singletonList(dto));
                logger.info("Inserted MatchDataDTO: {}", dto);

                // Signal the next sequence
                seq.signalNext();
            }
        }
    }

    /**
     * Retrieves the next run ID in a thread-safe manner.
     *
     * @return The next run ID.
     */
    private synchronized int getNextRunId() {
        return ++runCounter;
    }

    /**
     * Resets the run counter. Useful for testing to start run IDs from 1.
     */
    public synchronized void resetRunCounter() {
        runCounter = 0;
    }

    /**
     * Retrieves timestamps (min and max date_insert) for a given run ID.
     *
     * @param runId The run ID to query.
     * @return A map containing "min_date" and "max_date".
     */
    public Map<String, LocalDateTime> getTimestamps(String runId) {
        return matchDataRepository.getTimestamps(runId);
    }
}
