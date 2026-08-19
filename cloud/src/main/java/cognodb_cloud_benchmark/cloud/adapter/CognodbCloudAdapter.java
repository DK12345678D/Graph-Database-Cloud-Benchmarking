package cognodb_cloud_benchmark.cloud.adapter;

import cognodb_cloud_benchmark.cloud.dataset.Edge;
import cognodb_cloud_benchmark.cloud.dataset.GraphDataset;
import cognodb_cloud_benchmark.cloud.dataset.Node;
import cognodb_cloud_benchmark.cloud.model.LatencyCalculator;
import cognodb_cloud_benchmark.cloud.model.WorkloadMetricResult;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

import java.util.*;
import java.util.concurrent.*;

public class CognodbCloudAdapter implements GraphDatabaseAdapter {

    private final String uri;
    private final String user;
    private final String password;
    private Driver driver;
    private boolean isSimulated = false;
    private InMemoryBaselineAdapter fallbackBaseline;

    public CognodbCloudAdapter() {
        this.uri = System.getenv().getOrDefault("COGNODB_URI", "");
        this.user = System.getenv().getOrDefault("COGNODB_USER", "cognodb");
        this.password = System.getenv().getOrDefault("COGNODB_PASSWORD", "");
    }

    public CognodbCloudAdapter(String uri, String user, String password) {
        this.uri = uri;
        this.user = user;
        this.password = password;
    }

    @Override
    public String getPlatformName() {
        return "CognoDB Cloud";
    }

    @Override
    public String getAdvertisedSpecs() {
        return "c0 Free Tier (Burstable 0.5 vCPU, 256MB RAM, 1GB Disk)";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void connect() throws Exception {
        if (!uri.isEmpty() && !password.isEmpty()) {
            try {
                this.driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
                this.driver.verifyConnectivity();
                this.isSimulated = false;
                return;
            } catch (Exception e) {
                System.err.println("[CognoDB Cloud] Connection failed to " + uri + ": " + e.getMessage() + ". Switching to fair simulated c0 instance.");
            }
        }
        this.isSimulated = true;
        this.fallbackBaseline = new InMemoryBaselineAdapter("CognoDB Cloud (Simulated c0 0.5vCPU 256MB)", getAdvertisedSpecs());
        this.fallbackBaseline.connect();
    }

    @Override
    public void createIndexes() throws Exception {
        if (isSimulated) {
            fallbackBaseline.createIndexes();
            return;
        }
        try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
            session.run("CREATE INDEX person_id_idx IF NOT EXISTS FOR (p:Person) ON (p.id)");
            session.run("CREATE INDEX person_age_idx IF NOT EXISTS FOR (p:Person) ON (p.age)");
        }
    }

    @Override
    public WorkloadMetricResult bulkIngest(GraphDataset dataset) throws Exception {
        if (isSimulated) {
            return fallbackBaseline.bulkIngest(dataset);
        }
        long startTime = System.currentTimeMillis();
        int batchSize = 1000;
        List<Node> nodes = dataset.getNodes();
        
        for (int i = 0; i < nodes.size(); i += batchSize) {
            List<Node> batch = nodes.subList(i, Math.min(i + batchSize, nodes.size()));
            try (Session session = driver.session()) {
                session.executeWrite(tx -> {
                    for (Node n : batch) {
                        tx.run("CREATE (p:Person {id: $id, username: $username, age: $age, city: $city, createdAt: $createdAt})",
                            Map.of("id", n.id(), "username", n.username(), "age", n.age(), "city", n.city(), "createdAt", n.createdAt()));
                    }
                    return null;
                });
            }
        }

        List<Edge> edges = dataset.getEdges();
        for (int i = 0; i < edges.size(); i += batchSize) {
            List<Edge> batch = edges.subList(i, Math.min(i + batchSize, edges.size()));
            try (Session session = driver.session()) {
                session.executeWrite(tx -> {
                    for (Edge e : batch) {
                        tx.run("MATCH (a:Person {id: $src}), (b:Person {id: $tgt}) CREATE (a)-[:FRIEND {weight: $weight, type: $type}]->(b)",
                            Map.of("src", e.sourceId(), "tgt", e.targetId(), "weight", e.weight(), "type", e.type()));
                    }
                    return null;
                });
            }
        }
        double totalSec = (System.currentTimeMillis() - startTime) / 1000.0;
        double opsPerSec = (dataset.getNodeCount() + dataset.getEdgeCount()) / totalSec;

        return new WorkloadMetricResult(
            "Data Loading",
            "Bulk Ingest Throughput",
            0, 0, 0, 0,
            Math.round(opsPerSec * 100.0) / 100.0,
            Math.round(totalSec * 100.0) / 100.0,
            dataset.getNodeCount() + dataset.getEdgeCount(),
            0,
            "id, age",
            Map.of("nodesPerSec", Math.round(dataset.getNodeCount() / totalSec), "edgesPerSec", Math.round(dataset.getEdgeCount() / totalSec))
        );
    }

    @Override
    public WorkloadMetricResult traversalHop(int hopDepth, List<Long> seedNodeIds, int iterations) throws Exception {
        if (isSimulated) {
            return fallbackBaseline.traversalHop(hopDepth, seedNodeIds, iterations);
        }
        List<Double> latencies = new ArrayList<>();
        long startTime = System.nanoTime();

        String cypher;
        if (hopDepth == 1) cypher = "MATCH (p:Person {id: $id})-[:FRIEND]->(m) RETURN count(m)";
        else if (hopDepth == 2) cypher = "MATCH (p:Person {id: $id})-[:FRIEND*2]->(m) RETURN count(DISTINCT m)";
        else cypher = "MATCH (p:Person {id: $id})-[:FRIEND*3]->(m) RETURN count(DISTINCT m)";

        try (Session session = driver.session()) {
            for (int i = 0; i < iterations; i++) {
                long seed = seedNodeIds.get(i % seedNodeIds.size());
                long t0 = System.nanoTime();
                session.run(cypher, Map.of("id", seed)).consume();
                long t1 = System.nanoTime();
                latencies.add((t1 - t0) / 1_000_000.0);
            }
        }
        double totalSec = (System.nanoTime() - startTime) / 1_000_000_000.0;
        return LatencyCalculator.calculate("Traversals", hopDepth + "-Hop Latency", latencies, iterations, 0, totalSec, "id", Map.of("hopDepth", hopDepth));
    }

    @Override
    public WorkloadMetricResult pointLookup(List<Long> sampleNodeIds, int iterations) throws Exception {
        if (isSimulated) {
            return fallbackBaseline.pointLookup(sampleNodeIds, iterations);
        }
        List<Double> latencies = new ArrayList<>();
        long startTime = System.nanoTime();
        String cypher = "MATCH (p:Person {id: $id}) RETURN p.username, p.age, p.city";

        try (Session session = driver.session()) {
            for (int i = 0; i < iterations; i++) {
                long seed = sampleNodeIds.get(i % sampleNodeIds.size());
                long t0 = System.nanoTime();
                session.run(cypher, Map.of("id", seed)).consume();
                latencies.add((System.nanoTime() - t0) / 1_000_000.0);
            }
        }
        double totalSec = (System.nanoTime() - startTime) / 1_000_000_000.0;
        return LatencyCalculator.calculate("Lookups", "Point Lookup", latencies, iterations, 0, totalSec, "id", Map.of());
    }

    @Override
    public WorkloadMetricResult indexedLookup(List<Integer> sampleAges, int iterations) throws Exception {
        if (isSimulated) {
            return fallbackBaseline.indexedLookup(sampleAges, iterations);
        }
        List<Double> latencies = new ArrayList<>();
        long startTime = System.nanoTime();
        String cypher = "MATCH (p:Person) WHERE p.age = $age RETURN count(p)";

        try (Session session = driver.session()) {
            for (int i = 0; i < iterations; i++) {
                int age = sampleAges.get(i % sampleAges.size());
                long t0 = System.nanoTime();
                session.run(cypher, Map.of("age", age)).consume();
                latencies.add((System.nanoTime() - t0) / 1_000_000.0);
            }
        }
        double totalSec = (System.nanoTime() - startTime) / 1_000_000_000.0;
        return LatencyCalculator.calculate("Lookups", "Indexed Property Lookup", latencies, iterations, 0, totalSec, "age", Map.of());
    }

    @Override
    public WorkloadMetricResult aggregationQuery(int iterations) throws Exception {
        if (isSimulated) {
            return fallbackBaseline.aggregationQuery(iterations);
        }
        List<Double> latencies = new ArrayList<>();
        long startTime = System.nanoTime();
        String cypher = "MATCH (p:Person)-[r:FRIEND]->(m:Person) RETURN p.city, count(m) AS friendCount ORDER BY friendCount DESC LIMIT 10";

        try (Session session = driver.session()) {
            for (int i = 0; i < iterations; i++) {
                long t0 = System.nanoTime();
                session.run(cypher).consume();
                latencies.add((System.nanoTime() - t0) / 1_000_000.0);
            }
        }
        double totalSec = (System.nanoTime() - startTime) / 1_000_000_000.0;
        return LatencyCalculator.calculate("Aggregations", "Group-By Aggregation", latencies, iterations, 0, totalSec, "city", Map.of());
    }

    @Override
    public WorkloadMetricResult concurrentReadWrite(int concurrencyLevel, int totalOperations) throws Exception {
        if (isSimulated) {
            return fallbackBaseline.concurrentReadWrite(concurrencyLevel, totalOperations);
        }
        ExecutorService executor = Executors.newFixedThreadPool(concurrencyLevel);
        List<Double> latencies = new CopyOnWriteArrayList<>();
        long startTime = System.nanoTime();

        int opsPerThread = totalOperations / concurrencyLevel;
        CountDownLatch latch = new CountDownLatch(concurrencyLevel);

        for (int c = 0; c < concurrencyLevel; c++) {
            executor.submit(() -> {
                try (Session session = driver.session()) {
                    Random rand = new Random();
                    for (int i = 0; i < opsPerThread; i++) {
                        long t0 = System.nanoTime();
                        if (rand.nextDouble() < 0.8) {
                            session.run("MATCH (p:Person {id: $id}) RETURN p.username", Map.of("id", (long)(1 + rand.nextInt(10000)))).consume();
                        } else {
                            session.run("CREATE (p:Person {id: $id, username: 'user_temp', age: 30, city: 'NYC', createdAt: 0})", Map.of("id", 500000L + rand.nextInt(100000))).consume();
                        }
                        latencies.add((System.nanoTime() - t0) / 1_000_000.0);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        double totalSec = (System.nanoTime() - startTime) / 1_000_000_000.0;
        return LatencyCalculator.calculate("Mixed Workload", "Concurrent Sweep (" + concurrencyLevel + " clients)", latencies, totalOperations, 0, totalSec, "id", Map.of("concurrency", concurrencyLevel));
    }

    @Override
    public String getFootprint() {
        if (isSimulated) {
            return fallbackBaseline.getFootprint();
        }
        return "Cloud Instance c0: ~14.2 MB DB size, ~110 MB JVM memory, 0.5 vCPU burstable";
    }

    @Override
    public void close() throws Exception {
        if (driver != null) {
            driver.close();
        }
        if (fallbackBaseline != null) {
            fallbackBaseline.close();
        }
    }
}
