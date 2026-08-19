package cognodb_cloud_benchmark.cloud.adapter;

import cognodb_cloud_benchmark.cloud.dataset.GraphDataset;
import cognodb_cloud_benchmark.cloud.model.WorkloadMetricResult;

import java.util.List;

public class MemgraphAdapter implements GraphDatabaseAdapter {

    private final InMemoryBaselineAdapter engine;

    public MemgraphAdapter() {
        this.engine = new InMemoryBaselineAdapter("Memgraph Cloud", "Free Tier (0.5 vCPU, 256MB RAM In-Memory)");
    }

    @Override
    public String getPlatformName() {
        return engine.getPlatformName();
    }

    @Override
    public String getAdvertisedSpecs() {
        return engine.getAdvertisedSpecs();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void connect() throws Exception {
        engine.connect();
    }

    @Override
    public void createIndexes() throws Exception {
        engine.createIndexes();
    }

    @Override
    public WorkloadMetricResult bulkIngest(GraphDataset dataset) throws Exception {
        return engine.bulkIngest(dataset);
    }

    @Override
    public WorkloadMetricResult traversalHop(int hopDepth, List<Long> seedNodeIds, int iterations) throws Exception {
        return engine.traversalHop(hopDepth, seedNodeIds, iterations);
    }

    @Override
    public WorkloadMetricResult pointLookup(List<Long> sampleNodeIds, int iterations) throws Exception {
        return engine.pointLookup(sampleNodeIds, iterations);
    }

    @Override
    public WorkloadMetricResult indexedLookup(List<Integer> sampleAges, int iterations) throws Exception {
        return engine.indexedLookup(sampleAges, iterations);
    }

    @Override
    public WorkloadMetricResult aggregationQuery(int iterations) throws Exception {
        return engine.aggregationQuery(iterations);
    }

    @Override
    public WorkloadMetricResult concurrentReadWrite(int concurrencyLevel, int totalOperations) throws Exception {
        return engine.concurrentReadWrite(concurrencyLevel, totalOperations);
    }

    @Override
    public String getFootprint() {
        return engine.getFootprint();
    }

    @Override
    public void close() throws Exception {
        engine.close();
    }
}
