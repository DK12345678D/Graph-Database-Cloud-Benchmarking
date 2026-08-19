package cognodb_cloud_benchmark.cloud.dataset;

import java.util.*;

public class GraphDataset {
    private final List<Node> nodes;
    private final List<Edge> edges;
    private final Map<Long, Node> nodeMap;
    private final Map<Long, List<Edge>> adjacencyList;
    private final String name;
    private final String source;

    public GraphDataset(String name, String source, List<Node> nodes, List<Edge> edges) {
        this.name = name;
        this.source = source;
        this.nodes = Collections.unmodifiableList(nodes);
        this.edges = Collections.unmodifiableList(edges);
        
        Map<Long, Node> nMap = new HashMap<>();
        for (Node n : nodes) {
            nMap.put(n.id(), n);
        }
        this.nodeMap = Collections.unmodifiableMap(nMap);

        Map<Long, List<Edge>> adj = new HashMap<>();
        for (Edge e : edges) {
            adj.computeIfAbsent(e.sourceId(), k -> new ArrayList<>()).add(e);
        }
        this.adjacencyList = Collections.unmodifiableMap(adj);
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return source;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public Node getNode(long id) {
        return nodeMap.get(id);
    }

    public List<Edge> getOutgoingEdges(long sourceId) {
        return adjacencyList.getOrDefault(sourceId, Collections.emptyList());
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getEdgeCount() {
        return edges.size();
    }
}
