package cognodb_cloud_benchmark.cloud.model;

import java.util.List;
import java.util.Map;

public record BenchmarkResult(
    String platformName,
    String advertisedSpecs,
    List<WorkloadMetricResult> metrics,
    String footprint,
    boolean warmedUp,
    long timestamp,
    String status,
    String notes
) {}
