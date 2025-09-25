package com.boolean_brotherhood.public_transportation_journey_planner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.DataFilesRegistry;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final String DATA_PATH = "CapeTownTransitData/";

    private final TrainController trainController;
    private final TaxiController taxiController;
    private final MyCitiBusController busController;

    @Autowired
    public AdminController(
            TrainController trainController,
            TaxiController taxiController,
            MyCitiBusController busController
    ) {
        this.trainController = trainController;
        this.taxiController = taxiController;
        this.busController = busController;
    }

    /**
     * List all files & subfolders in CapeTownTransitData
     */
    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles(@RequestParam(required = false) String subPath) throws IOException {
        SystemLog.log_endpoint("/api/admin/list");
        String targetPath = DATA_PATH + (subPath != null ? subPath : "");
        Resource resource = new ClassPathResource(targetPath);

        if (!resource.exists()) {
            SystemLog.log_event("ADMIN", "Requested data path not found", "WARN", Map.of(
                    "targetPath", targetPath
            ));
            return ResponseEntity.notFound().build();
        }

        List<String> fileNames = new ArrayList<>();
        File[] files = resource.getFile().listFiles();
        if (files != null) {
            for (File res : files) {
                fileNames.add(res.getName());
            }
        }

        SystemLog.log_event("ADMIN", "Listed data files", "INFO", Map.of(
                "targetPath", targetPath,
                "count", fileNames.size()
        ));

        return ResponseEntity.ok(fileNames);
    }

    /**
     * Read a specific file and return its contents as text
     */
    @GetMapping("/file")
    public ResponseEntity<String> readFile(@RequestParam String filePath) throws IOException {
        SystemLog.log_endpoint("/api/admin/file");
        Resource resource = new ClassPathResource(DATA_PATH + filePath);

        if (!resource.exists()) {
            SystemLog.log_event("ADMIN", "Requested data file not found", "WARN", Map.of(
                    "filePath", filePath
            ));
            return ResponseEntity.notFound().build();
        }

        String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        SystemLog.log_event("ADMIN", "Read data file", "INFO", Map.of(
                "filePath", filePath,
                "size", content.length()
        ));
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    /**
     * Download a specific file
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filePath) throws IOException {
        SystemLog.log_endpoint("/api/admin/download");
        Resource resource = new ClassPathResource(DATA_PATH + filePath);

        if (!resource.exists()) {
            SystemLog.log_event("ADMIN", "Requested download missing", "WARN", Map.of(
                    "filePath", filePath
            ));
            return ResponseEntity.notFound().build();
        }

        SystemLog.log_event("ADMIN", "Prepared file download", "INFO", Map.of(
                "filePath", filePath,
                "fileName", resource.getFilename()
        ));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * Collect metrics from all controllers
     */
    @GetMapping("/systemMetrics")
    public Map<String, Object> getAllMetrics() {
        SystemLog.log_endpoint("/api/admin/systemMetrics");
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("train", trainController.getMetrics());
        metrics.put("taxi", taxiController.getMetrics());
        metrics.put("bus", busController.getMetrics());
        SystemLog.log_event("ADMIN", "Collected subsystem metrics", "INFO", Map.of(
                "sections", metrics.keySet().size()
        ));
        return metrics;
    }
    
    @GetMapping("/GetFileInUse")
    public Map<String, String> getFilesInUse() {
        SystemLog.log_endpoint("/api/admin/GetFileInUse");
        Map<String, String> usage = DataFilesRegistry.getUsageLogs();
        SystemLog.log_event("ADMIN", "Fetched files-in-use registry", "INFO", Map.of(
                "entries", usage.size()
        ));
        return usage;
    }

    @GetMapping("/MostRecentCall")
    public List<Map<String, Object>> getMostRecentCalls() {
        SystemLog.log_endpoint("/api/admin/MostRecentCall");
        List<Map<String, Object>> recent = SystemLog.GET_ENDPOINT_DATA();
        SystemLog.log_event("ADMIN", "Requested endpoint call history", "INFO", Map.of(
                "count", recent.size()
        ));
        return recent;
    }

    @GetMapping("/systemLogs")
    public List<Map<String, Object>> getSystemLogs(@RequestParam(name = "limit", defaultValue = "100") int limit) {
        SystemLog.log_endpoint("/api/admin/systemLogs");
        int safeLimit = Math.max(1, limit);
        List<Map<String, Object>> logs = SystemLog.GET_SYSTEM_EVENTS(safeLimit);
        SystemLog.log_event("ADMIN", "Retrieved system event snapshot", "INFO", Map.of(
                "requestedLimit", limit,
                "returned", logs.size()
        ));
        return logs;
    }

    /**
     * Replace an existing file with a new uploaded file.
     * If the file does not exist, it will be created.
     */
    @PostMapping("/replaceFile")
    public ResponseEntity<String> replaceFile(
            @RequestParam String filePath,
            @RequestPart("file") MultipartFile file
    ) {
        SystemLog.log_endpoint("/api/admin/replaceFile");
        try {
            Path target = Paths.get(DATA_PATH, filePath).normalize();
            Files.createDirectories(target.getParent());
            Files.write(target, file.getBytes());

            SystemLog.log_event("ADMIN", "Replaced data file", "INFO", Map.of(
                    "filePath", target.toString(),
                    "size", file.getSize()
            ));

            return ResponseEntity.ok("File replaced successfully: " + target.getFileName());
        } catch (IOException e) {
            SystemLog.log_event("ADMIN", "Failed to replace file", "ERROR", Map.of(
                    "filePath", filePath,
                    "error", e.getMessage()
            ));
            return ResponseEntity.status(500).body("Error replacing file: " + e.getMessage());
        }
    }

    /**
     * Update a file's contents by appending new content.
     * If the file does not exist, it will be created.
     */
    @PostMapping("/updateFile")
    public ResponseEntity<String> updateFile(
            @RequestParam String filePath,
            @RequestParam String content
    ) {
        SystemLog.log_endpoint("/api/admin/updateFile");
        try {
            Path target = Paths.get(DATA_PATH, filePath).normalize();
            Files.createDirectories(target.getParent());
            Files.write(target, content.getBytes(StandardCharsets.UTF_8), 
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            SystemLog.log_event("ADMIN", "Updated data file", "INFO", Map.of(
                    "filePath", target.toString(),
                    "appendLength", content.length()
            ));

            return ResponseEntity.ok("File updated successfully: " + target.getFileName());
        } catch (IOException e) {
            SystemLog.log_event("ADMIN", "Failed to update file", "ERROR", Map.of(
                    "filePath", filePath,
                    "error", e.getMessage()
            ));
            return ResponseEntity.status(500).body("Error updating file: " + e.getMessage());
        }
    }
}
