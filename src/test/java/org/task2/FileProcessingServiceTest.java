package org.task2;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.task2.services.FileProcessingService;

@QuarkusTest
public class FileProcessingServiceTest {


    @Inject
    FileProcessingService fileProcessingService;

    @Test
    public void processFileTest() {
        fileProcessingService.processFileStreamUsingCopy("test.txt");
    }

}
