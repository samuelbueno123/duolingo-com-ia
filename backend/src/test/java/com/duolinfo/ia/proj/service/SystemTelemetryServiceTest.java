package com.duolinfo.ia.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemTelemetryServiceTest {

    @Test
    @DisplayName("Distingue vCPUs de hardware (logicalProcessors) das threads da JVM e registra CPU Sistema/JVM")
    void shouldReportHardwareLogicalProcessorsSeparatelyFromJvmThreads() throws InterruptedException {
        SystemTelemetryService service = new SystemTelemetryService();
        service.warmUpTelemetry();
        Thread.sleep(60L);

        Map<String, Object> snapshot = service.captureSnapshot(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> jvm = (Map<String, Object>) snapshot.get("jvm");

        int expectedHardwareThreads = Runtime.getRuntime().availableProcessors();
        int reportedLogicalProcessors = ((Number) jvm.get("logicalProcessors")).intValue();
        int reportedAvailableProcessors = ((Number) jvm.get("availableProcessors")).intValue();
        int jvmActiveThreads = ((Number) jvm.get("jvmActiveThreads")).intValue();
        int threadCount = ((Number) jvm.get("threadCount")).intValue();
        double sysCpu = ((Number) jvm.get("systemCpuPercent")).doubleValue();
        double procCpu = ((Number) jvm.get("processCpuPercent")).doubleValue();

        assertEquals(expectedHardwareThreads, reportedLogicalProcessors);
        assertEquals(expectedHardwareThreads, reportedAvailableProcessors);
        assertEquals(threadCount, jvmActiveThreads);
        assertTrue(jvmActiveThreads > 0);
        assertTrue(sysCpu >= 0.0 && sysCpu <= 100.0);
        assertTrue(procCpu >= 0.0 && procCpu <= 100.0);

        List<Map<String, Object>> history = service.getHistorySnapshot();
        assertFalse(history.isEmpty());
        Map<String, Object> latest = history.get(history.size() - 1);
        assertTrue(latest.containsKey("systemCpuPercent"));
        assertTrue(latest.containsKey("processCpuPercent"));
        assertEquals(expectedHardwareThreads, ((Number) latest.get("logicalProcessors")).intValue());
    }
}
