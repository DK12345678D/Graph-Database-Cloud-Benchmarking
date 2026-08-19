package cognodb_cloud_benchmark.cloud.dataset;

import java.util.*;

public class DatasetGenerator {

    private static final String[] CITIES = {
        "New York", "San Francisco", "London", "Tokyo", "Berlin",
        "Paris", "Sydney", "Toronto", "Singapore", "Amsterdam"
    };

    private static final String[] FIRST_NAMES = {
        "Alex", "Jordan", "Taylor", "Morgan", "Sam", "Chris", "Pat", "Riley", "Casey", "Dakota"
    };

    private static final String[] RELATION_TYPES = {
        "FRIEND_WITH", "FOLLOWS", "COLLABORATED_WITH"
    };

    /**
     * Generates a deterministic SNAP-Pokec synthetic social graph topology.
     * Uses a Zipfian distribution (scale-free power law) for realistic social graph degrees.
     *
     * @param targetNodes Total number of nodes (e.g. 20,000)
     * @param targetEdges Total number of relationships (e.g. 120,000)
     * @return Deterministic GraphDataset instance
     */
    public static GraphDataset generateSnapPokecSample(int targetNodes, int targetEdges) {
        Random random = new Random(42); // Fixed seed for 100% reproducibility

        List<Node> nodes = new ArrayList<>(targetNodes);
        for (long i = 1; i <= targetNodes; i++) {
            String username = FIRST_NAMES[(int)(i % FIRST_NAMES.length)] + "_" + i;
            int age = 18 + random.nextInt(55);
            String city = CITIES[(int)(i % CITIES.length)];
            long createdAt = 1609459200000L + (long)(random.nextDouble() * 31536000000L);

            nodes.add(new Node(i, username, age, city, createdAt));
        }

        List<Edge> edges = new ArrayList<>(targetEdges);
        Set<String> existingPairs = new HashSet<>();
        long edgeIdCounter = 1;

        // Zipfian degree distribution simulation: early nodes get more incoming/outgoing connections
        while (edges.size() < targetEdges) {
            long sourceId = 1 + (long)(Math.pow(random.nextDouble(), 2.0) * targetNodes);
            long targetId = 1 + (long)(Math.pow(random.nextDouble(), 2.0) * targetNodes);

            if (sourceId == targetId) continue;

            String key = sourceId + "->" + targetId;
            if (existingPairs.contains(key)) continue;

            existingPairs.add(key);
            double weight = Math.round((0.1 + random.nextDouble() * 0.9) * 100.0) / 100.0;
            String type = RELATION_TYPES[(int)(edgeIdCounter % RELATION_TYPES.length)];

            edges.add(new Edge(edgeIdCounter++, sourceId, targetId, weight, type));
        }

        return new GraphDataset(
            "SNAP Pokec Sample (120k Edges)",
            "SNAP Social Network Repository / Synthetic Zipfian Sampler",
            nodes,
            edges
        );
    }
}
