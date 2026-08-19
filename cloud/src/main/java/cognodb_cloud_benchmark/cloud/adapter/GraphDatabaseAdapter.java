package cognodb_cloud_benchmark.cloud.adapter;

import cognodb_cloud_benchmark.cloud.dataset.GraphDataset;
import cognodb_cloud_benchmark.cloud.model.WorkloadMetricResult;

import java.util.List;

public interface GraphDatabaseAdapter extends AutoCloseable {
    String getPlatformName();
    String getAdvertisedSpecs();
    boolean isAvailable();
    
    void connect() throws Exception;
    void createIndexes() throws Exception;
    
    WorkloadMetricResult bulkIngest(GraphDataset dataset) throws Exception;
    WorkloadMetricResult traversalHop(int hopDepth, List<Long> seedNodeIds, int iterations) throws Exception;
    WorkloadMetricResult pointLookup(List<Long> sampleNodeIds, int iterations) throws Exception;
    WorkloadMetricResult indexedLookup(List<Integer> sampleAges, int iterations) throws Exception;
    WorkloadMetricResult aggregationQuery(int iterations) throws Exception;
    WorkloadMetricResult concurrentReadWrite(int concurrencyLevel, int totalOperations) throws Exception;
    
    String getFootprint();
    void close() throws Exception;
}
