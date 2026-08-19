package cognodb_cloud_benchmark.cloud.service;

import cognodb_cloud_benchmark.cloud.adapter.*;
import cognodb_cloud_benchmark.cloud.dataset.DatasetGenerator;
import cognodb_cloud_benchmark.cloud.dataset.GraphDataset;
import cognodb_cloud_benchmark.cloud.model.BenchmarkResult;
import cognodb_cloud_benchmark.cloud.model.WorkloadMetricResult;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BenchmarkRunnerService {

    private final List<String> executionLogs = new ArrayList<>();
    private Map<String, BenchmarkResult> latestResults = new LinkedHashMap<>();

    public synchronized Map<String, BenchmarkResult> runFullBenchmark(int nodeCount, int edgeCount, int iterations) {
        log("==========================================================================");
        log("STARTING GRAPH DATABASE CLOUD BENCHMARK SUITE");
        log("Dataset: SNAP Pokec Sample (" + edgeCount + " Edges, " + nodeCount + " Nodes)");
        log("Iteration count per workload: " + iterations + " (After warm-up)");
        log("Resource Target: Fair Tier Parity (0.5 vCPU, 256MB RAM equivalent)");
        log("==========================================================================");

        GraphDataset dataset = DatasetGenerator.generateSnapPokecSample(nodeCount, edgeCount);

        List<GraphDatabaseAdapter> adapters = List.of(
            new CognodbCloudAdapter(),
            new Neo4jAdapter(),
            new MemgraphAdapter(),
            new ArangoDbAdapter(),
            new FalkorDbAdapter(),
            new InMemoryBaselineAdapter()
        );

        Map<String, BenchmarkResult> resultMap = new LinkedHashMap<>();

        // Generate sample seed lists
        Random random = new Random(42);
        List<Long> seedNodes = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            seedNodes.add(1L + random.nextInt(nodeCount));
        }

        List<Integer> sampleAges = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            sampleAges.add(18 + random.nextInt(55));
        }

        for (GraphDatabaseAdapter adapter : adapters) {
            String platform = adapter.getPlatformName();
            log("\n---> Benchmarking Platform: " + platform + " (" + adapter.getAdvertisedSpecs() + ")");
            List<WorkloadMetricResult> metrics = new ArrayList<>();

            try {
                adapter.connect();
                adapter.createIndexes();

                // 1. Data Loading
                log("  [1/6] Ingesting dataset (" + dataset.getNodeCount() + " nodes, " + dataset.getEdgeCount() + " edges)...");
                WorkloadMetricResult ingestResult = adapter.bulkIngest(dataset);
                metrics.add(ingestResult);
                log("        Ingest Completed: " + ingestResult.opsPerSec() + " ops/sec, Wall-Clock: " + ingestResult.totalDurationSec() + "s");

                // Warm-up phase
                log("  [Warm-Up] Warming up query caches (50 iterations)...");
                adapter.traversalHop(1, seedNodes, 50);
                adapter.pointLookup(seedNodes, 50);

                // 2. Traversals (1-hop, 2-hop, 3-hop)
                log("  [2/6] Running Traversals (1-hop, 2-hop, 3-hop)...");
                WorkloadMetricResult trav1 = adapter.traversalHop(1, seedNodes, iterations);
                WorkloadMetricResult trav2 = adapter.traversalHop(2, seedNodes, iterations);
                WorkloadMetricResult trav3 = adapter.traversalHop(3, seedNodes, iterations);
                metrics.addAll(List.of(trav1, trav2, trav3));
                log("        1-Hop p50: " + trav1.p50Ms() + "ms, p95: " + trav1.p95Ms() + "ms");
                log("        2-Hop p50: " + trav2.p50Ms() + "ms, p95: " + trav2.p95Ms() + "ms");
                log("        3-Hop p50: " + trav3.p50Ms() + "ms, p95: " + trav3.p95Ms() + "ms");

                // 3. Lookups (Point & Indexed)
                log("  [3/6] Running Lookups (Point ID & Indexed Age)...");
                WorkloadMetricResult lookupPoint = adapter.pointLookup(seedNodes, iterations);
                WorkloadMetricResult lookupIndex = adapter.indexedLookup(sampleAges, iterations);
                metrics.addAll(List.of(lookupPoint, lookupIndex));
                log("        Point Lookup p50: " + lookupPoint.p50Ms() + "ms, p95: " + lookupPoint.p95Ms() + "ms");
                log("        Indexed Lookup p50: " + lookupIndex.p50Ms() + "ms, p95: " + lookupIndex.p95Ms() + "ms");

                // 4. Aggregations
                log("  [4/6] Running Aggregations (Group-By Count)...");
                WorkloadMetricResult aggResult = adapter.aggregationQuery(iterations);
                metrics.add(aggResult);
                log("        Aggregation p50: " + aggResult.p50Ms() + "ms, p95: " + aggResult.p95Ms() + "ms");

                // 5. Mixed Workload Concurrency Sweep (1, 10, 40 clients)
                log("  [5/6] Running Mixed Workload Concurrency Sweeps (1, 10, 40 clients)...");
                WorkloadMetricResult conc1 = adapter.concurrentReadWrite(1, 500);
                WorkloadMetricResult conc10 = adapter.concurrentReadWrite(10, 1000);
                WorkloadMetricResult conc40 = adapter.concurrentReadWrite(40, 2000);
                metrics.addAll(List.of(conc1, conc10, conc40));
                log("        Sustained QPS @ 1 client: " + conc1.opsPerSec() + " QPS (p95: " + conc1.p95Ms() + "ms)");
                log("        Sustained QPS @ 10 clients: " + conc10.opsPerSec() + " QPS (p95: " + conc10.p95Ms() + "ms)");
                log("        Sustained QPS @ 40 clients: " + conc40.opsPerSec() + " QPS (p95: " + conc40.p95Ms() + "ms)");

                // 6. Footprint
                String footprint = adapter.getFootprint();
                log("  [6/6] Resource Footprint: " + footprint);

                BenchmarkResult result = new BenchmarkResult(
                    platform,
                    adapter.getAdvertisedSpecs(),
                    metrics,
                    footprint,
                    true,
                    System.currentTimeMillis(),
                    "SUCCESS",
                    "Completed cleanly under fair hardware tier constraints"
                );
                resultMap.put(platform, result);

                adapter.close();
            } catch (Exception e) {
                log("  [ERROR] Benchmark failed for " + platform + ": " + e.getMessage());
                resultMap.put(platform, new BenchmarkResult(
                    platform, adapter.getAdvertisedSpecs(), Collections.emptyList(), "N/A", false, System.currentTimeMillis(), "FAILED", e.getMessage()
                ));
            }
        }

        this.latestResults = resultMap;
        log("\n==========================================================================");
        log("BENCHMARK SUITE COMPLETE - ALL METRICS CAPTURED FOR " + resultMap.size() + " PLATFORMS");
        log("==========================================================================");

        return resultMap;
    }

    public Map<String, BenchmarkResult> getLatestResults() {
        if (latestResults.isEmpty()) {
            runFullBenchmark(20000, 120000, 100);
        }
        return latestResults;
    }

    public List<String> getExecutionLogs() {
        return Collections.unmodifiableList(executionLogs);
    }

    private void log(String msg) {
        System.out.println(msg);
        executionLogs.add(msg);
    }

    public String generateMarkdownResultsTable(Map<String, BenchmarkResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Benchmark Results Matrix (Fair Hardware Parity: ~0.5 vCPU, 256MB RAM)\n\n");
        sb.append("| Platform | Specs Tier | Ingest Rate | 1-Hop p50/p95 | 2-Hop p50/p95 | 3-Hop p50/p95 | Point Lookup p50/p95 | Indexed Lookup p50/p95 | Group-By p50/p95 | QPS @ 40 Clients |\n");
        sb.append("|---|---|---|---|---|---|---|---|---|---|\n");

        for (BenchmarkResult res : results.values()) {
            Map<String, WorkloadMetricResult> metricMap = new HashMap<>();
            for (WorkloadMetricResult m : res.metrics()) {
                metricMap.put(m.metricName(), m);
            }

            WorkloadMetricResult ing = metricMap.get("Bulk Ingest Throughput");
            WorkloadMetricResult t1 = metricMap.get("1-Hop Latency");
            WorkloadMetricResult t2 = metricMap.get("2-Hop Latency");
            WorkloadMetricResult t3 = metricMap.get("3-Hop Latency");
            WorkloadMetricResult pl = metricMap.get("Point Lookup");
            WorkloadMetricResult il = metricMap.get("Indexed Property Lookup");
            WorkloadMetricResult agg = metricMap.get("Group-By Aggregation");
            WorkloadMetricResult c40 = metricMap.get("Concurrent Sweep (40 clients)");

            sb.append(String.format(Locale.US,
                "| **%s** | %s | %s ops/s (%.1fs) | %.2f / %.2f ms | %.2f / %.2f ms | %.2f / %.2f ms | %.2f / %.2f ms | %.2f / %.2f ms | %.2f / %.2f ms | %.1f QPS |\n",
                res.platformName(),
                res.advertisedSpecs(),
                ing != null ? String.format(Locale.US, "%.0f", ing.opsPerSec()) : "N/A",
                ing != null ? ing.totalDurationSec() : 0.0,
                t1 != null ? t1.p50Ms() : 0, t1 != null ? t1.p95Ms() : 0,
                t2 != null ? t2.p50Ms() : 0, t2 != null ? t2.p95Ms() : 0,
                t3 != null ? t3.p50Ms() : 0, t3 != null ? t3.p95Ms() : 0,
                pl != null ? pl.p50Ms() : 0, pl != null ? pl.p95Ms() : 0,
                il != null ? il.p50Ms() : 0, il != null ? il.p95Ms() : 0,
                agg != null ? agg.p50Ms() : 0, agg != null ? agg.p95Ms() : 0,
                c40 != null ? c40.opsPerSec() : 0
            ));
        }

        return sb.toString();
    }
}
