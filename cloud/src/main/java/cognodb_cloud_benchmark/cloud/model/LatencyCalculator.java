package cognodb_cloud_benchmark.cloud.model;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;
import java.util.Map;

public class LatencyCalculator {

    public static WorkloadMetricResult calculate(
        String category,
        String metricName,
        List<Double> latenciesMs,
        long totalOps,
        long failedOps,
        double wallClockSec,
        String indexedProps,
        Map<String, Object> extraMeta
    ) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Double lat : latenciesMs) {
            stats.addValue(lat);
        }

        double p50 = latenciesMs.isEmpty() ? 0.0 : stats.getPercentile(50);
        double p95 = latenciesMs.isEmpty() ? 0.0 : stats.getPercentile(95);
        double p99 = latenciesMs.isEmpty() ? 0.0 : stats.getPercentile(99);
        double mean = latenciesMs.isEmpty() ? 0.0 : stats.getMean();
        double opsPerSec = wallClockSec > 0 ? (totalOps - failedOps) / wallClockSec : 0.0;

        return new WorkloadMetricResult(
            category,
            metricName,
            round(p50),
            round(p95),
            round(p99),
            round(mean),
            round(opsPerSec),
            round(wallClockSec),
            totalOps,
            failedOps,
            indexedProps != null ? indexedProps : "N/A",
            extraMeta != null ? extraMeta : Map.of()
        );
    }

    private static double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
