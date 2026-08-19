package cognodb_cloud_benchmark.cloud.dataset;

public record Node(
    long id,
    String username,
    int age,
    String city,
    long createdAt
) {}
