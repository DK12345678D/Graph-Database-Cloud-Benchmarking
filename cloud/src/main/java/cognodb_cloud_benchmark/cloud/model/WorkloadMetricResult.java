package cognodb_cloud_benchmark.cloud.model;

import java.util.Map;

public record WorkloadMetricResult(
    String category,
    String metricName,
    double p50Ms,
    double p95Ms,
    double p99Ms,
    double meanMs,
    double opsPerSec,
    double totalDurationSec,
    long totalOperations,
    long failedOperations,
    String indexedProperties,
    Map<String, Object> extraMetadata
) {}
