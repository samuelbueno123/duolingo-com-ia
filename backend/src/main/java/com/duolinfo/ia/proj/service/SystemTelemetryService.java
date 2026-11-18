package com.duolinfo.ia.proj.service;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.duolinfo.ia.proj.util.SupportedLanguages;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.annotation.PostConstruct;

/**
 * Coleta métricas reais de execução (HTTP, CPU de sistema/JVM, Heap, Threads Java vs vCPUs de hardware,
 * GC, HikariCP e distribuição por idioma) com histórico contínuo em ring buffer.
 */
@Service
public class SystemTelemetryService {

    private static final int MAX_HISTORY_POINTS = 30;
    private static final int MAX_LATENCY_WINDOW = 200;
    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong success2xx = new AtomicLong();
    private final AtomicLong client4xx = new AtomicLong();
    private final AtomicLong server5xx = new AtomicLong();
    private final AtomicLong totalLatencyMs = new AtomicLong();
    private final AtomicLong maxLatencyMs = new AtomicLong();
    private final AtomicInteger activeInFlight = new AtomicInteger();

    private final Deque<Long> latencyWindow = new ArrayDeque<>(MAX_LATENCY_WINDOW);
    private final Deque<Map<String, Object>> historyRing = new ArrayDeque<>(MAX_HISTORY_POINTS);
    private final Object historyLock = new Object();

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private SseEmitterService sseEmitterService;

    private volatile long lastRequestSnapshotAtMs = 0L;
    private volatile long lastRequestCountAtSample = 0L;

    // Fallback por delta de tempo de CPU do processo caso getProcessCpuLoad() retorne -1 na primeira leitura
    private final Object cpuSampleLock = new Object();
    private long prevProcessCpuTimeNs = -1L;
    private long prevCpuSampleWallNs = 0L;
    private double lastValidProcessCpuPercent = 0.0;
    private double lastValidSystemCpuPercent = 0.0;

    @PostConstruct
    public void warmUpTelemetry() {
        try {
            // Primeira leitura aquece os contadores do OperatingSystemMXBean para cálculos delta precisos
            readSystemAndProcessCpu();
            captureSnapshot(true);
        } catch (Exception ignored) {
        }
    }

    @Scheduled(fixedRate = 5000, initialDelay = 5000)
    public void captureScheduledSample() {
        try {
            captureSnapshot(true);
        } catch (Exception ignored) {
        }
    }

    public void onRequestStart() {
        activeInFlight.incrementAndGet();
    }

    public void onRequestComplete(int status, long durationMs) {
        activeInFlight.updateAndGet(cur -> Math.max(0, cur - 1));
        long safeDuration = Math.max(0L, durationMs);

        long reqCount = totalRequests.incrementAndGet();
        totalLatencyMs.addAndGet(safeDuration);
        maxLatencyMs.accumulateAndGet(safeDuration, Math::max);

        if (status >= 500) {
            server5xx.incrementAndGet();
        } else if (status >= 400) {
            client4xx.incrementAndGet();
        } else if (status >= 200 && status < 400) {
            success2xx.incrementAndGet();
        }

        synchronized (historyLock) {
            if (latencyWindow.size() >= MAX_LATENCY_WINDOW) {
                latencyWindow.removeFirst();
            }
            latencyWindow.addLast(safeDuration);

            long now = System.currentTimeMillis();
            if (now - lastRequestSnapshotAtMs >= 5000L && reqCount > lastRequestCountAtSample) {
                recordLightSnapshot(now);
            }
        }
    }

    public Map<String, Object> captureSnapshot(boolean forceRecord) {
        double dbPingMs = measureDbPingMs();
        Map<String, Object> jvm = readJvmMetrics();
        Map<String, Object> pool = readDatabasePoolMetrics(dbPingMs);
        Map<String, Object> http = readHttpMetrics();

        long now = System.currentTimeMillis();
        synchronized (historyLock) {
            if (forceRecord || historyRing.isEmpty() || now - lastRequestSnapshotAtMs >= 2000L) {
                appendHistoryPointLocked(now, http, jvm, pool, dbPingMs);
            }
        }

        Map<String, Object> combined = new LinkedHashMap<>();
        combined.put("http", http);
        combined.put("jvm", jvm);
        combined.put("pool", pool);
        return combined;
    }

    public List<Map<String, Object>> getHistorySnapshot() {
        synchronized (historyLock) {
            return new ArrayList<>(historyRing);
        }
    }

    public Map<String, Object> readHttpMetrics() {
        long total = totalRequests.get();
        long s2xx = success2xx.get();
        long c4xx = client4xx.get();
        long e5xx = server5xx.get();
        double avgMs = total == 0 ? 0.0 : round1((double) totalLatencyMs.get() / total);
        double p95Ms = computeP95LatencyMs();
        double errorRate = total == 0 ? 0.0 : round2(((c4xx + e5xx) * 100.0) / total);
        double serverErrorRate = total == 0 ? 0.0 : round2((e5xx * 100.0) / total);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalRequests", total);
        map.put("success2xx", s2xx);
        map.put("client4xx", c4xx);
        map.put("server5xx", e5xx);
        map.put("activeInFlight", activeInFlight.get());
        map.put("avgLatencyMs", avgMs);
        map.put("p95LatencyMs", p95Ms);
        map.put("maxLatencyMs", maxLatencyMs.get());
        map.put("errorRatePercent", errorRate);
        map.put("serverErrorRatePercent", serverErrorRate);
        return map;
    }

    public Map<String, Object> readJvmMetrics() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        long heapUsedMb = heap.getUsed() / (1024 * 1024);
        long heapCommittedMb = heap.getCommitted() / (1024 * 1024);
        long heapMaxMb = (heap.getMax() > 0 ? heap.getMax() : Runtime.getRuntime().maxMemory()) / (1024 * 1024);
        double heapUsedPct = heapMaxMb > 0 ? round1((heapUsedMb * 100.0) / heapMaxMb) : 0.0;
        double heapCommittedPct = heapCommittedMb > 0 ? round1((heapUsedMb * 100.0) / heapCommittedMb) : 0.0;
        long nonHeapUsedMb = Math.max(0L, nonHeap.getUsed() / (1024 * 1024));

        long gcCount = 0;
        long gcTimeMs = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionCount() > 0) gcCount += gc.getCollectionCount();
            if (gc.getCollectionTime() > 0) gcTimeMs += gc.getCollectionTime();
        }

        int logicalCpus = Math.max(1, Runtime.getRuntime().availableProcessors());
        double[] cpuLoads = readSystemAndProcessCpu();
        double systemCpuPct = cpuLoads[0];
        double cpuProcessPct = cpuLoads[1];
        double systemLoadAvg = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();

        int jvmActiveThreads = threadMXBean.getThreadCount();
        long hostTotalMemMb = 0L;
        long hostFreeMemMb = 0L;
        double hostMemUsedPct = 0.0;
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            long totalBytes = sunOs.getTotalMemorySize();
            long freeBytes = sunOs.getFreeMemorySize();
            if (totalBytes > 0) {
                hostTotalMemMb = totalBytes / (1024 * 1024);
                hostFreeMemMb = Math.max(0L, freeBytes / (1024 * 1024));
                long usedBytes = Math.max(0L, totalBytes - Math.max(0L, freeBytes));
                hostMemUsedPct = round1((usedBytes * 100.0) / totalBytes);
            }
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("heapUsedMb", heapUsedMb);
        map.put("heapCommittedMb", heapCommittedMb);
        map.put("heapMaxMb", heapMaxMb);
        map.put("heapUsedPercent", heapUsedPct);
        map.put("heapCommittedPercent", heapCommittedPct);
        map.put("nonHeapUsedMb", nonHeapUsedMb);
        map.put("threadCount", jvmActiveThreads);
        map.put("jvmActiveThreads", jvmActiveThreads);
        map.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        map.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
        map.put("gcCount", gcCount);
        map.put("gcTimeMs", gcTimeMs);
        map.put("processCpuPercent", cpuProcessPct);
        map.put("systemCpuPercent", systemCpuPct);
        map.put("systemLoadAverage", systemLoadAvg >= 0 ? round2(systemLoadAvg) : 0.0);
        map.put("availableProcessors", logicalCpus);
        map.put("logicalProcessors", logicalCpus);
        map.put("hostTotalMemoryMb", hostTotalMemMb);
        map.put("hostFreeMemoryMb", hostFreeMemMb);
        map.put("hostMemoryUsedPercent", hostMemUsedPct);
        return map;
    }

    public Map<String, Object> readDatabasePoolMetrics(double dbPingMs) {
        Map<String, Object> pool = new LinkedHashMap<>();
        pool.put("pingMs", round2(dbPingMs));
        pool.put("status", dbPingMs >= 0 ? "UP" : "DOWN");

        if (dataSource instanceof HikariDataSource hikari) {
            pool.put("poolName", hikari.getPoolName());
            pool.put("maximumPoolSize", hikari.getMaximumPoolSize());
            pool.put("minimumIdle", hikari.getMinimumIdle());
            if (hikari.getHikariPoolMXBean() != null) {
                int active = hikari.getHikariPoolMXBean().getActiveConnections();
                int idle = hikari.getHikariPoolMXBean().getIdleConnections();
                int total = hikari.getHikariPoolMXBean().getTotalConnections();
                int awaiting = hikari.getHikariPoolMXBean().getThreadsAwaitingConnection();
                int max = Math.max(1, hikari.getMaximumPoolSize());
                pool.put("activeConnections", active);
                pool.put("idleConnections", idle);
                pool.put("totalConnections", total);
                pool.put("threadsAwaitingConnection", awaiting);
                pool.put("utilizationPercent", round1((active * 100.0) / max));
            }
        }
        return pool;
    }

    public Map<String, Object> readRealtimeMetrics() {
        Map<String, Object> rt = new LinkedHashMap<>();
        int sseConnections = sseEmitterService != null ? sseEmitterService.getConnectedUsersCount() : 0;
        rt.put("sseConnectedUsers", sseConnections);
        return rt;
    }

    public List<Map<String, Object>> buildCanonicalLanguageBreakdown() {
        Map<String, Long> studentCounts = queryLanguageCountMap(
            "SELECT language_name, COUNT(*) FROM student_languages GROUP BY language_name"
        );
        Map<String, Long> teacherCounts = queryLanguageCountMap(
            "SELECT language_name, COUNT(*) FROM teacher_taught_languages GROUP BY language_name"
        );
        Map<String, Long> classCounts = queryLanguageCountMap(
            "SELECT language_name, COUNT(*) FROM classes WHERE active = TRUE GROUP BY language_name"
        );
        Map<String, Long> exerciseCounts = queryLanguageCountMap(
            "SELECT language_name, COUNT(*) FROM learning_exercises WHERE active = TRUE GROUP BY language_name"
        );

        List<Map<String, Object>> breakdown = new ArrayList<>();
        for (String canonical : SupportedLanguages.ALL) {
            String key = SupportedLanguages.stripAccentsLower(canonical);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("language", canonical);
            row.put("students", studentCounts.getOrDefault(key, 0L));
            row.put("teachers", teacherCounts.getOrDefault(key, 0L));
            row.put("classes", classCounts.getOrDefault(key, 0L));
            row.put("exercises", exerciseCounts.getOrDefault(key, 0L));
            breakdown.add(row);
        }
        return breakdown;
    }

    private Map<String, Long> queryLanguageCountMap(String sql) {
        Map<String, Long> counts = new LinkedHashMap<>();
        if (jdbcTemplate == null) {
            return counts;
        }
        try {
            jdbcTemplate.query(sql, rs -> {
                String raw = rs.getString(1);
                long cnt = rs.getLong(2);
                SupportedLanguages.normalize(raw).ifPresent(canonical ->
                    counts.merge(SupportedLanguages.stripAccentsLower(canonical), cnt, Long::sum)
                );
            });
        } catch (Exception ignored) {
        }
        return counts;
    }

    private void recordLightSnapshot(long now) {
        Map<String, Object> http = readHttpMetrics();
        Map<String, Object> jvm = readJvmMetrics();
        Map<String, Object> pool = readDatabasePoolMetrics(0.0);
        appendHistoryPointLocked(now, http, jvm, pool, 0.0);
    }

    private void appendHistoryPointLocked(
            long now,
            Map<String, Object> http,
            Map<String, Object> jvm,
            Map<String, Object> pool,
            double dbPingMs) {
        long currentTotalReqs = totalRequests.get();
        long reqDelta = Math.max(0L, currentTotalReqs - lastRequestCountAtSample);
        lastRequestSnapshotAtMs = now;
        lastRequestCountAtSample = currentTotalReqs;

        Map<String, Object> point = new LinkedHashMap<>();
        point.put("timestampMs", now);
        point.put("label", LocalTime.now().format(LABEL_FORMATTER));
        point.put("heapUsedPercent", jvm.get("heapUsedPercent"));
        point.put("heapUsedMb", jvm.get("heapUsedMb"));
        point.put("processCpuPercent", jvm.get("processCpuPercent"));
        point.put("systemCpuPercent", jvm.get("systemCpuPercent"));
        point.put("avgLatencyMs", http.get("avgLatencyMs"));
        point.put("p95LatencyMs", http.get("p95LatencyMs"));
        point.put("dbPingMs", Math.max(0.0, round2(dbPingMs)));
        point.put("threadCount", jvm.get("threadCount"));
        point.put("logicalProcessors", jvm.get("logicalProcessors"));
        point.put("requestsDelta", reqDelta);
        point.put("totalRequests", currentTotalReqs);
        point.put("hikariActive", pool.getOrDefault("activeConnections", 0));

        if (historyRing.size() >= MAX_HISTORY_POINTS) {
            historyRing.removeFirst();
        }
        historyRing.addLast(point);
    }

    private double measureDbPingMs() {
        if (jdbcTemplate == null) {
            return -1.0;
        }
        try {
            long startNs = System.nanoTime();
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return (System.nanoTime() - startNs) / 1_000_000.0;
        } catch (Exception e) {
            return -1.0;
        }
    }

    private double computeP95LatencyMs() {
        List<Long> sorted;
        synchronized (historyLock) {
            if (latencyWindow.isEmpty()) {
                return 0.0;
            }
            sorted = new ArrayList<>(latencyWindow);
        }
        sorted.sort(Long::compareTo);
        int idx = (int) Math.ceil(sorted.size() * 0.95) - 1;
        idx = Math.max(0, Math.min(idx, sorted.size() - 1));
        return sorted.get(idx).doubleValue();
    }

    private double[] readSystemAndProcessCpu() {
        synchronized (cpuSampleLock) {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            int logicalCpus = Math.max(1, Runtime.getRuntime().availableProcessors());
            double processPct = lastValidProcessCpuPercent;
            double systemPct = lastValidSystemCpuPercent;

            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                double rawProc = sunOs.getProcessCpuLoad();
                if (rawProc >= 0.0 && !Double.isNaN(rawProc)) {
                    processPct = clampPercent(round1(rawProc * 100.0));
                    lastValidProcessCpuPercent = processPct;
                } else {
                    long nowNs = System.nanoTime();
                    long procTimeNs = sunOs.getProcessCpuTime();
                    if (procTimeNs >= 0 && prevProcessCpuTimeNs >= 0 && nowNs > prevCpuSampleWallNs) {
                        double deltaProc = procTimeNs - prevProcessCpuTimeNs;
                        double deltaWall = nowNs - prevCpuSampleWallNs;
                        processPct = clampPercent(round1((deltaProc / (deltaWall * logicalCpus)) * 100.0));
                        lastValidProcessCpuPercent = processPct;
                    }
                    prevProcessCpuTimeNs = procTimeNs;
                    prevCpuSampleWallNs = nowNs;
                }

                double rawSys = sunOs.getCpuLoad();
                if (rawSys >= 0.0 && !Double.isNaN(rawSys)) {
                    systemPct = clampPercent(round1(rawSys * 100.0));
                    lastValidSystemCpuPercent = systemPct;
                } else {
                    double loadAvg = osBean.getSystemLoadAverage();
                    if (loadAvg >= 0.0) {
                        systemPct = clampPercent(round1((loadAvg / logicalCpus) * 100.0));
                        lastValidSystemCpuPercent = systemPct;
                    } else {
                        systemPct = Math.max(systemPct, processPct);
                    }
                }
            }
            return new double[] { systemPct, processPct };
        }
    }

    private static double clampPercent(double val) {
        return Math.max(0.0, Math.min(100.0, val));
    }

    private static double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    private static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
