package com.boolean_brotherhood.public_transportation_journey_planner;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.time.LocalDateTime;

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
    private final GABusController gaBusController;

    @Autowired
    public AdminController(
            TrainController trainController,
            TaxiController taxiController,
            MyCitiBusController busController,
            GABusController gaBusController
    ) {
        this.trainController = trainController;
        this.taxiController = taxiController;
        this.busController = busController;
        this.gaBusController = gaBusController;
    }


    /**
     * List all files & subfolders in CapeTownTransitData
     */
    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles(@RequestParam(required = false) String subPath) throws IOException {
        SystemLog.log_endpoint("/api/admin/list");
        String normalizedSubPath = (subPath != null && !subPath.isBlank()) ? subPath.replace("\\", "/") : null;
        if (normalizedSubPath != null && normalizedSubPath.startsWith(DATA_PATH)) {
            normalizedSubPath = normalizedSubPath.substring(DATA_PATH.length());
        }
        String targetFragment = normalizedSubPath != null ? normalizedSubPath.replaceFirst("^/+", "") : "";
        String sanitizedPrefix = sanitizePrefix(targetFragment);
        String targetPath = DATA_PATH + targetFragment;
        Resource resource = new ClassPathResource(targetPath);

        List<String> fileNames = new ArrayList<>();
        try {
            if (!resource.exists()) {
                SystemLog.log_event("ADMIN", "Requested data path not found", "WARN", Map.of(
                        "targetPath", targetPath
                ));
                return ResponseEntity.ok(getKnownDataFiles(subPath));
            }

            if (isJarResource(resource)) {
                fileNames = listFromJar(resource, sanitizedPrefix);
            } else {
                File resourceFile = resource.getFile();
                fileNames = collectEntries(resourceFile.toPath(), sanitizedPrefix);
            }
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            SystemLog.log_event("ADMIN", "File listing failed, using fallback", "WARN", Map.of(
                    "targetPath", targetPath,
                    "error", errorMessage
            ));
            return ResponseEntity.ok(getKnownDataFiles(subPath));
        }

        if (fileNames.isEmpty()) {
            List<String> fallback = getKnownDataFiles(subPath);
            SystemLog.log_event("ADMIN", "File listing empty, using fallback", "WARN", Map.of(
                    "targetPath", targetPath,
                    "fallbackCount", fallback.size()
            ));
            return ResponseEntity.ok(fallback);
        }

        SystemLog.log_event("ADMIN", "Listed data files", "INFO", Map.of(
                "targetPath", targetPath,
                "count", fileNames.size()
        ));

        return ResponseEntity.ok(fileNames);
    }

    // Add this helper method
    private List<String> getKnownDataFiles(String subPath) {
        if (subPath == null || subPath.isEmpty()) {
            return Arrays.asList(
                "Train/", 
                "Taxi/", 
                "MyCitiBus/", 
                "stops.csv", 
                "routes.txt",
                "schedules.txt"
            );
        }
        
        // Define subfolder contents
        switch (subPath) {
            case "trains/":
                return Arrays.asList("stops.txt", "trips.txt", "routes.txt");
            case "buses/":
                return Arrays.asList("myciti_stops.csv", "myciti_trips.csv", "ga_stops.csv");
            case "taxis/":
                return Arrays.asList("taxi_routes.csv", "taxi_stops.csv");
            default:
                return new ArrayList<>();
        }
    }



    private String sanitizePrefix(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String cleaned = path.trim();
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    private boolean isJarResource(Resource resource) throws IOException {
        return resource.getURI().toString().startsWith("jar:");
    }

    private List<String> listFromJar(Resource resource, String prefix) throws IOException {
        URI uri = resource.getURI();
        String uriText = uri.toString();
        int separatorIndex = uriText.indexOf("!/");
        if (separatorIndex < 0) {
            return new ArrayList<>();
        }

        URI jarUri = URI.create(uriText.substring(0, separatorIndex));
        String internalPath = uriText.substring(separatorIndex + 2);
        if (internalPath.isEmpty()) {
            internalPath = "/";
        }

        FileSystem fileSystem = null;
        boolean shouldClose = false;
        try {
            try {
                fileSystem = FileSystems.getFileSystem(jarUri);
            } catch (FileSystemNotFoundException ex) {
                fileSystem = FileSystems.newFileSystem(jarUri, Map.of());
                shouldClose = true;
            }

            Path rootPath = fileSystem.getPath(internalPath);
            if (!Files.exists(rootPath)) {
                rootPath = fileSystem.getPath("/").resolve(internalPath).normalize();
            }
            return collectEntries(rootPath, prefix);
        } finally {
            if (shouldClose && fileSystem != null) {
                fileSystem.close();
            }
        }
    }

    private List<String> collectEntries(Path rootPath, String prefix) throws IOException {
        Set<String> entries = new LinkedHashSet<>();
        if (!Files.exists(rootPath)) {
            return new ArrayList<>(entries);
        }

        if (Files.isDirectory(rootPath)) {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(rootPath)) {
                for (Path child : dirStream) {
                    String name = child.getFileName().toString();
                    String display = (prefix != null && !prefix.isEmpty()) ? prefix + "/" + name : name;
                    display = display.replace('\'', '/');
                    if (Files.isDirectory(child)) {
                        if (!display.endsWith("/")) {
                            display = display + "/";
                        }
                    }
                    entries.add(display);
                }
            }

            try (Stream<Path> stream = Files.walk(rootPath)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    Path relative = rootPath.relativize(path);
                    String entry = relative.toString().replace('\'', '/');
                    if (prefix != null && !prefix.isEmpty()) {
                        entry = prefix + "/" + entry;
                    }
                    entries.add(entry);
                });
            }
        } else if (Files.isRegularFile(rootPath)) {
            String entry = rootPath.getFileName().toString();
            if (prefix != null && !prefix.isEmpty()) {
                entry = prefix + "/" + entry;
            }
            entries.add(entry.replace('\'', '/'));
        }

        return new ArrayList<>(entries);
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
        metrics.put("ga", gaBusController.getMetrics());
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

    @GetMapping("/performance")
    public Map<String, Object> getPerformanceMetrics() {
        SystemLog.log_endpoint("/api/admin/performance");
        Map<String, Object> payload = new HashMap<>();
        payload.put("generatedAt", LocalDateTime.now().toString());
        payload.put("overview", PerformanceMetricsRegistry.getOverview());
        payload.put("perEndpoint", PerformanceMetricsRegistry.getEndpointSummaries());
        payload.put("recent", PerformanceMetricsRegistry.getRecentSamples());
        SystemLog.log_event("ADMIN", "Collected performance metrics", "INFO", Map.of("samples", ((java.util.List<?>) payload.get("recent")).size()));
        return payload;
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
