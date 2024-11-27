package org.task2.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
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
    @Operation(summary = "Process a file", description = "Processes the specified file for match data insertion.")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "File processed successfully"),
            @APIResponse(responseCode = "400", description = "File name must be provided"),
            @APIResponse(responseCode = "500", description = "Error while processing file")
    })
    @Path("/process")
    public Response processFile(
            @Parameter(description = "Name of the file to process", required = true)
            @QueryParam("fileName") String fileName) {
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
    @Operation(summary = "Retrieve Timestamps", description = "Retrieves the minimum and maximum insertion timestamps for a given run ID.")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Timestamps retrieved successfully"),
            @APIResponse(responseCode = "400", description = "Bad Request - Run ID not provided or invalid"),
            @APIResponse(responseCode = "404", description = "Not Found - No timestamps found for the given run ID")
    })
    @Path("/timestamps")
    public Response getTimestamps(
            @Parameter(description = "Run ID to retrieve timestamps for", required = true)
            @QueryParam("runId") String runId) {
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
