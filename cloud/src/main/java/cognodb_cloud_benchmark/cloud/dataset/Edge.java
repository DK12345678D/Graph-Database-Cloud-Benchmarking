package cognodb_cloud_benchmark.cloud.dataset;

public record Edge(
    long id,
    long sourceId,
    long targetId,
    double weight,
    String type
) {}
