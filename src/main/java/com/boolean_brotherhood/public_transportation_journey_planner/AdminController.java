package com.boolean_brotherhood.public_transportation_journey_planner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        String targetPath = DATA_PATH + (subPath != null ? subPath : "");
        Resource resource = new ClassPathResource(targetPath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        List<String> fileNames = new ArrayList<>();
        File[] files = resource.getFile().listFiles();
        if (files != null) {
            for (File res : files) {
                fileNames.add(res.getName());
            }
        }

        return ResponseEntity.ok(fileNames);
    }

    /**
     * Read a specific file and return its contents as text
     */
    @GetMapping("/file")
    public ResponseEntity<String> readFile(@RequestParam String filePath) throws IOException {
        Resource resource = new ClassPathResource(DATA_PATH + filePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    /**
     * Download a specific file
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filePath) throws IOException {
        Resource resource = new ClassPathResource(DATA_PATH + filePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

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
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("train", trainController.getMetrics());
        metrics.put("taxi", taxiController.getMetrics());
        metrics.put("bus", busController.getMetrics());
        return metrics;
    }
    
    @GetMapping("/GetFileInUse")
    public Map<String, String> getFilesInUse() {
        return DataFilesRegistry.getUsageLogs();
    }

}
