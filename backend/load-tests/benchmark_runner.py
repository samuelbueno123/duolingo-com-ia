#!/usr/bin/env python3
"""
Duolinfo Scalability & Benchmark CLI Runner
Executes concurrent load tests against Duolinfo endpoints and prints SLA latency distribution.
Calculates: Mean, Median (P50), P90, P95, P99, Throughput (RPS), Error Rate.
"""

import argparse
import concurrent.futures
import json
import statistics
import sys
import time
import urllib.request
import urllib.error

def execute_request(url, method='GET', headers=None, data=None):
    if headers is None:
        headers = {'Accept': 'application/json'}
    req = urllib.request.Request(url, headers=headers, method=method)
    if data:
        req.data = data.encode('utf-8')
    
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            duration_ms = (time.perf_counter() - start) * 1000.0
            return (resp.status, duration_ms, True)
    except urllib.error.HTTPError as e:
        duration_ms = (time.perf_counter() - start) * 1000.0
        # Allow 401 or 403 as valid auth responses if expected
        return (e.code, duration_ms, e.code < 500)
    except Exception as e:
        duration_ms = (time.perf_counter() - start) * 1000.0
        return (0, duration_ms, False)

def run_benchmark(url, total_requests=100, concurrency=10, method='GET', headers=None, data=None):
    print(f"\n=======================================================")
    print(f"Benchmark: {method} {url}")
    print(f"Total Requests: {total_requests} | Concurrency: {concurrency}")
    print(f"=======================================================")
    
    latencies = []
    errors = 0
    start_time = time.perf_counter()

    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(execute_request, url, method, headers, data) for _ in range(total_requests)]
        for f in concurrent.futures.as_completed(futures):
            status, duration, ok = f.result()
            latencies.append(duration)
            if not ok:
                errors += 1

    total_duration = time.perf_counter() - start_time
    rps = total_requests / (total_duration if total_duration > 0 else 0.001)
    sorted_latencies = sorted(latencies)

    def p(pct):
        if not sorted_latencies:
            return 0.0
        idx = int(round(pct * len(sorted_latencies))) - 1
        return sorted_latencies[max(0, min(idx, len(sorted_latencies) - 1))]

    mean_lat = statistics.mean(latencies) if latencies else 0
    p50 = statistics.median(latencies) if latencies else 0
    p90 = p(0.90)
    p95 = p(0.95)
    p99 = p(0.99)
    err_pct = (errors / total_requests) * 100.0

    print(f"Resultado do Benchmark:")
    print(f"  - Tempo Total:      {total_duration:.2f} s")
    print(f"  - Throughput (RPS): {rps:.1f} req/s")
    print(f"  - Taxa de Erros:    {errors}/{total_requests} ({err_pct:.2f}%)")
    print(f"  - Latência Média:   {mean_lat:.2f} ms")
    print(f"  - Mediana (P50):    {p50:.2f} ms")
    print(f"  - P90:              {p90:.2f} ms")
    print(f"  - P95:              {p95:.2f} ms")
    print(f"  - P99:              {p99:.2f} ms")
    print(f"=======================================================\n")

    return {
        'url': url,
        'total_requests': total_requests,
        'concurrency': concurrency,
        'duration_sec': total_duration,
        'rps': rps,
        'error_rate_pct': err_pct,
        'mean_ms': mean_lat,
        'p50_ms': p50,
        'p90_ms': p90,
        'p95_ms': p95,
        'p99_ms': p99
    }

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Duolinfo Load Benchmark Tool")
    parser.add_argument('--url', default='http://localhost:8080/api/catalog/languages', help='Target URL')
    parser.add_argument('--requests', type=int, default=100, help='Total requests')
    parser.add_argument('--concurrency', type=int, default=10, help='Concurrency level')
    args = parser.parse_args()

    run_benchmark(args.url, args.requests, args.concurrency)
