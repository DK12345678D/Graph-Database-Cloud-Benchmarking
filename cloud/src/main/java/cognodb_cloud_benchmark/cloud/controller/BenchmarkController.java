package cognodb_cloud_benchmark.cloud.controller;

import cognodb_cloud_benchmark.cloud.model.BenchmarkResult;
import cognodb_cloud_benchmark.cloud.service.BenchmarkRunnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/benchmark")
@CrossOrigin(origins = "*")
public class BenchmarkController {

    private final BenchmarkRunnerService benchmarkRunnerService;

    public BenchmarkController(BenchmarkRunnerService benchmarkRunnerService) {
        this.benchmarkRunnerService = benchmarkRunnerService;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, BenchmarkResult>> runBenchmark(
            @RequestParam(defaultValue = "20000") int nodes,
            @RequestParam(defaultValue = "120000") int edges,
            @RequestParam(defaultValue = "100") int iterations
    ) {
        Map<String, BenchmarkResult> results = benchmarkRunnerService.runFullBenchmark(nodes, edges, iterations);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/results")
    public ResponseEntity<Map<String, BenchmarkResult>> getResults() {
        return ResponseEntity.ok(benchmarkRunnerService.getLatestResults());
    }

    @GetMapping("/logs")
    public ResponseEntity<List<String>> getLogs() {
        return ResponseEntity.ok(benchmarkRunnerService.getExecutionLogs());
    }

    @GetMapping("/export/markdown")
    public ResponseEntity<String> exportMarkdown() {
        Map<String, BenchmarkResult> results = benchmarkRunnerService.getLatestResults();
        String markdown = benchmarkRunnerService.generateMarkdownResultsTable(results);
        return ResponseEntity.ok(markdown);
    }
}
