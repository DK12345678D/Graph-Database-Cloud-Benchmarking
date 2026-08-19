package cognodb_cloud_benchmark.cloud.cli;

import cognodb_cloud_benchmark.cloud.model.BenchmarkResult;
import cognodb_cloud_benchmark.cloud.service.BenchmarkRunnerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;

@Component
public class BenchmarkCliRunner implements CommandLineRunner {

    private final BenchmarkRunnerService benchmarkRunnerService;

    public BenchmarkCliRunner(BenchmarkRunnerService benchmarkRunnerService) {
        this.benchmarkRunnerService = benchmarkRunnerService;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean autoRun = false;
        for (String arg : args) {
            if (arg.contains("--benchmark.auto=true") || arg.equalsIgnoreCase("--cli")) {
                autoRun = true;
                break;
            }
        }

        if (autoRun) {
            System.out.println(">>> CLI Mode Activated: Running Graph Database Cloud Benchmark Suite...");
            Map<String, BenchmarkResult> results = benchmarkRunnerService.runFullBenchmark(20000, 120000, 100);
            String markdown = benchmarkRunnerService.generateMarkdownResultsTable(results);

            try (PrintWriter out = new PrintWriter(new FileWriter("BENCHMARK_RESULTS.md"))) {
                out.println(markdown);
            }
            System.out.println(">>> Results matrix exported to BENCHMARK_RESULTS.md cleanly!");
        }
    }
}
