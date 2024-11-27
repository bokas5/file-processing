package org.task2.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.task2.services.FileProcessingService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Path("/file-processing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FileStreamResource {

    @Inject
    FileProcessingService fileProcessingService;

    @POST
    @Path("/process")
    public Response processFile(@QueryParam("fileName") String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("File name must be provided")
                    .build();
        }
        try {
            fileProcessingService.processFileStreamUsingCopy(fileName);
            return Response.ok("File processed successfully").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error while processing file: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/timestamps")
    public Response getTimestamps(@QueryParam("runId") String runId) {
        if (runId == null || runId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Run ID must be provided")
                    .build();
        }

        Map<String, LocalDateTime> timestamps = fileProcessingService.getTimestamps(runId);
        LocalDateTime minDate = timestamps.get("min_date");
        LocalDateTime maxDate = timestamps.get("max_date");

        Map<String, Object> response = new HashMap<>();
        response.put("min_date", minDate);
        response.put("max_date", maxDate);

        if (minDate != null && maxDate != null) {
            Duration duration = Duration.between(minDate, maxDate);
            double durationSeconds = duration.getSeconds() + duration.getNano() / 1_000_000_000.0;
            response.put("duration_seconds", durationSeconds);
        }

        return Response.ok(response).build();
    }
}
