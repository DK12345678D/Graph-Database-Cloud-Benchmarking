# Benchmark Results Matrix (Fair Hardware Parity: ~0.5 vCPU, 256MB RAM)

| Platform | Specs Tier | Ingest Rate | 1-Hop p50/p95 | 2-Hop p50/p95 | 3-Hop p50/p95 | Point Lookup p50/p95 | Indexed Lookup p50/p95 | Group-By p50/p95 | QPS @ 40 Clients |
|---|---|---|---|---|---|---|---|---|---|
| **CognoDB Cloud** | c0 Free Tier (Burstable 0.5 vCPU, 256MB RAM, 1GB Disk) | 93583 ops/s (1.5s) | 0.42 / 0.50 ms | 1.75 / 2.06 ms | 8.55 / 11.08 ms | 0.19 / 0.22 ms | 0.41 / 0.46 ms | 14.70 / 37.53 ms | 47525.1 QPS |
| **Neo4j AuraDB Free** | Free Tier (1 vCPU, 2GB RAM, 200k nodes limit) | 72314 ops/s (1.9s) | 0.50 / 0.66 ms | 2.22 / 2.60 ms | 10.94 / 13.23 ms | 0.24 / 0.28 ms | 0.53 / 0.59 ms | 25.34 / 41.83 ms | 119800.7 QPS |
| **Memgraph Cloud** | Free Tier (0.5 vCPU, 256MB RAM In-Memory) | 101981 ops/s (1.4s) | 0.36 / 0.46 ms | 1.57 / 1.74 ms | 7.74 / 9.26 ms | 0.17 / 0.20 ms | 0.38 / 0.42 ms | 14.82 / 27.63 ms | 73337.4 QPS |
| **ArangoDB Oasis** | Free Tier (0.5 vCPU, 256MB RAM Multi-Model) | 63636 ops/s (2.2s) | 0.59 / 0.73 ms | 2.52 / 2.67 ms | 12.23 / 14.21 ms | 0.28 / 0.32 ms | 0.61 / 0.67 ms | 28.25 / 49.14 ms | 59673.2 QPS |
| **FalkorDB / RedisGraph** | Free Tier (0.5 vCPU, 256MB RAM Redis Module) | 88384 ops/s (1.6s) | 0.44 / 0.54 ms | 1.81 / 2.06 ms | 8.87 / 10.98 ms | 0.20 / 0.23 ms | 0.43 / 0.48 ms | 17.25 / 29.34 ms | 180799.1 QPS |
| **In-Memory Graph Engine (Reference Baseline)** | Cap: 0.5 vCPU, 256MB RAM | 79545 ops/s (1.8s) | 0.50 / 0.59 ms | 1.97 / 2.11 ms | 9.79 / 11.21 ms | 0.22 / 0.26 ms | 0.47 / 0.53 ms | 18.84 / 32.12 ms | 103717.8 QPS |

