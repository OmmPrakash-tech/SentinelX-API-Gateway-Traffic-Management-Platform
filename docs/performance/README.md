# Measured local performance

These are observed development-machine runs, not production capacity guarantees. The gateway and four demo services ran natively on the same Windows host, with a persistent H2 database, authenticated API-key requests, request-log persistence, concurrency 20, and no external network hop. Each stage follows the previous stage, so JIT warmup and machine state affect comparisons. No warmup samples were discarded. CPU/memory values are Windows gateway-process snapshots, not whole-system resource accounting.

## Rate policies temporarily disabled; authentication, persistence and gateway enabled

Recorded 2026-09-14T10:06:38.585Z. Machine: 13th Gen Intel(R) Core(TM) i5-13420H; 12 logical CPUs; Windows_NT 10.0.26200; Node v24.14.1.

| Requests | HTTP statuses | Requests/s | p50 ms | p95 ms | p99 ms | Error rate | Gateway CPU s | End working set MiB |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 100 | 200: 100 | 321.8 | 45.87 | 127.09 | 136.97 | 0.00% | 2.44 | 262.0 |
| 1000 | 200: 1000 | 679.5 | 27.06 | 53.88 | 90.85 | 0.00% | 11.42 | 285.8 |
| 5000 | 200: 5000 | 1088.0 | 17.31 | 23.58 | 54.72 | 0.00% | 32.06 | 333.7 |
| 10000 | 200: 10000 | 1589.2 | 12.39 | 19.42 | 33.19 | 0.00% | 35.84 | 390.2 |

Raw report: [2026-09-14T10-06-38-585Z-unthrottled.json](2026-09-14T10-06-38-585Z-unthrottled.json).

## Configured rate policies enabled

Recorded 2026-09-14T10:10:13.610Z. Machine: 13th Gen Intel(R) Core(TM) i5-13420H; 12 logical CPUs; Windows_NT 10.0.26200; Node v24.14.1.

| Requests | HTTP statuses | Requests/s | p50 ms | p95 ms | p99 ms | Error rate | Gateway CPU s | End working set MiB |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 100 | 200: 100 | 367.1 | 35.86 | 121.96 | 145.73 | 0.00% | 2.53 | 430.7 |
| 1000 | 200: 1000 | 1026.0 | 15.39 | 42.81 | 53.43 | 0.00% | 4.28 | 428.2 |
| 5000 | 200: 690, 429: 4310 | 1941.4 | 8.95 | 23.45 | 33.11 | 86.20% | 13.34 | 498.2 |
| 10000 | 200: 725, 429: 9275 | 2684.1 | 5.82 | 17.60 | 30.74 | 92.75% | 12.98 | 495.3 |

Raw report: [2026-09-14T10-10-13-610Z-policies-enabled.json](2026-09-14T10-10-13-610Z-policies-enabled.json).

## Rate policies temporarily disabled; authentication, persistence and gateway enabled

Recorded 2026-09-14T15:14:20.115Z. Machine: 13th Gen Intel(R) Core(TM) i5-13420H; 12 logical CPUs; Windows_NT 10.0.26200; Node v24.14.1.

| Requests | HTTP statuses | Requests/s | p50 ms | p95 ms | p99 ms | Error rate | Gateway CPU s | End working set MiB |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 100 | 200: 100 | 352.6 | 42.94 | 107.71 | 112.05 | 0.00% | 2.28 | 265.3 |
| 1000 | 200: 1000 | 540.9 | 34.28 | 64.80 | 116.43 | 0.00% | 13.75 | 294.2 |
| 5000 | 200: 5000 | 882.0 | 21.30 | 32.35 | 64.89 | 0.00% | 37.45 | 375.9 |
| 10000 | 200: 10000 | 1500.8 | 12.37 | 21.48 | 38.19 | 0.00% | 34.14 | 458.8 |

Raw report: [2026-09-14T15-14-20-115Z-unthrottled.json](2026-09-14T15-14-20-115Z-unthrottled.json).

## Interpretation

The rate-enabled error column includes expected HTTP 429 rejections. It is not an upstream outage percentage. Because rejected requests do not execute the downstream call, their throughput is not comparable to successful forwarding throughput. The unthrottled script temporarily disabled the five rate policies and restored every original value in its finally block. Both scripts revoked their temporary key and logged out. The UI now includes the real test traffic, including deliberate failures and rejections.

The final 10,000-request forwarding run observed approximately 1,501 requests/s at p95 21.48 ms with zero errors. The earlier 1,589 requests/s observation is retained separately in its timestamped report. This is one local sample with a very small response and a local H2 database. It does not establish PostgreSQL, multi-host, Internet, sustained-load, saturation, or AWS performance.

Reproduce with `scripts/run-load-test.ps1` or `scripts/run-load-test.ps1 -Unthrottled`. `LOAD_COUNTS`, `LOAD_CONCURRENCY`, `BASE_URL` and `DEMO_PASSWORD` can customize the run. Unthrottled mode requires the local demo administrator and should only be used in an isolated environment. Review saved settings if a process is forcibly terminated before its cleanup runs.

## Failure/recovery evidence

See [chaos-results.json](chaos-results.json) for actual live-stack assertions: all three routes, two-instance distribution, health exclusion/recovery, transient GET retry, no POST retry, timeout, and circuit open/block/recovery. These controls are enabled only in native development and require the generated control token.
