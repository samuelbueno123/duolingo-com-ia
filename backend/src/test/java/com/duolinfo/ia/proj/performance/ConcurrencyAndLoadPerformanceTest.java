package com.duolinfo.ia.proj.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.repository.StudentRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class ConcurrencyAndLoadPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyAndLoadPerformanceTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    public static class BenchmarkResult {
        public double meanMs;
        public double medianMs;
        public double p90Ms;
        public double p95Ms;
        public double p99Ms;
        public double rps;
        public double errorRatePct;
        public int totalRequests;
        public int errors;
        public double totalDurationSec;

        @Override
        public String toString() {
            return String.format(
                "Total: %d reqs | Erros: %d (%.2f%%) | Duração: %.2fs | RPS: %.1f | Média: %.2fms | P50: %.2fms | P90: %.2fms | P95: %.2fms | P99: %.2fms",
                totalRequests, errors, errorRatePct, totalDurationSec, rps, meanMs, medianMs, p90Ms, p95Ms, p99Ms
            );
        }
    }

    private BenchmarkResult runConcurrentBenchmark(int totalRequests, int concurrency, RequestCallable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalRequests);
        List<Double> latenciesMs = Collections.synchronizedList(new ArrayList<>(totalRequests));
        AtomicInteger errorCount = new AtomicInteger(0);
        SecurityContext context = SecurityContextHolder.getContext();

        long overallStart = System.nanoTime();

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                if (context != null) {
                    SecurityContextHolder.setContext(context);
                }
                try {
                    startLatch.await();
                    long reqStart = System.nanoTime();
                    boolean success = task.execute();
                    long reqEnd = System.nanoTime();
                    double latencyMs = (reqEnd - reqStart) / 1_000_000.0;
                    latenciesMs.add(latencyMs);
                    if (!success) {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await();
        long overallEnd = System.nanoTime();
        executor.shutdown();

        double totalDurationSec = (overallEnd - overallStart) / 1_000_000_000.0;

        List<Double> sortedLatencies = new ArrayList<>(latenciesMs);
        Collections.sort(sortedLatencies);

        BenchmarkResult res = new BenchmarkResult();
        res.totalRequests = totalRequests;
        res.errors = errorCount.get();
        res.totalDurationSec = totalDurationSec;
        res.errorRatePct = (res.errors / (double) totalRequests) * 100.0;
        res.rps = totalRequests / (totalDurationSec > 0 ? totalDurationSec : 0.001);

        double sum = 0;
        for (Double l : sortedLatencies) {
            sum += l;
        }
        res.meanMs = sortedLatencies.isEmpty() ? 0 : sum / sortedLatencies.size();
        res.medianMs = getPercentile(sortedLatencies, 0.50);
        res.p90Ms = getPercentile(sortedLatencies, 0.90);
        res.p95Ms = getPercentile(sortedLatencies, 0.95);
        res.p99Ms = getPercentile(sortedLatencies, 0.99);

        return res;
    }

    private double getPercentile(List<Double> sortedList, double percentile) {
        if (sortedList.isEmpty()) return 0;
        int idx = (int) Math.ceil(percentile * sortedList.size()) - 1;
        if (idx < 0) idx = 0;
        if (idx >= sortedList.size()) idx = sortedList.size() - 1;
        return sortedList.get(idx);
    }

    @FunctionalInterface
    private interface RequestCallable {
        boolean execute() throws Exception;
    }

    @Test
    @DisplayName("Teste de Carga: Consulta concorrente de catálogo com Spring Cache ativo (/api/catalog/languages)")
    void testCatalogLanguagesLoadAndCache() throws Exception {
        // Primeiro acesso (cache warm-up)
        mockMvc.perform(get("/api/catalog/languages")).andExpect(status().isOk());

        // Executa 100 requisições simultâneas com 10 threads concorrentes
        BenchmarkResult result = runConcurrentBenchmark(100, 10, () -> {
            int status = mockMvc.perform(get("/api/catalog/languages"))
                .andReturn().getResponse().getStatus();
            return status == 200;
        });

        log.info("[BENCHMARK-CATALOG-CACHE] {}", result);
        assertEquals(0, result.errors, "Taxa de erro deve ser 0% para catálogo em cache");
        assertTrue(result.meanMs < 25.0, "Latência média com Spring Cache deve ser inferior a 25ms em memória");
    }

    @Test
    @DisplayName("Teste de Carga: Consulta concorrente de sessão e dados da tela Home (/api/home)")
    void testHomeDataConcurrentLoad() throws Exception {
        // Warm-up
        mockMvc.perform(get("/api/home").with(user("student_test@duolinfo.com").roles("STUDENT")))
            .andExpect(status().isOk());

        BenchmarkResult result = runConcurrentBenchmark(100, 10, () -> {
            int status = mockMvc.perform(get("/api/home").with(user("student_test@duolinfo.com").roles("STUDENT")))
                .andReturn().getResponse().getStatus();
            return status == 200;
        });

        log.info("[BENCHMARK-HOME-SESSION] {}", result);
        assertEquals(0, result.errors, "Taxa de erro deve ser 0%");
        assertTrue(result.p95Ms < 60.0, "P95 para consulta de sessão em memória deve ser < 60ms");
    }

    @Test
    @DisplayName("Teste de Carga: Carregamento do perfil e idiomas do estudante via EntityGraph otimizado")
    void testStudentProfileEntityGraphPerformance() throws Exception {
        // Cria estudante de teste se não existir
        String email = "perf_student@example.com";
        studentRepository.findByEmail(email).orElseGet(() -> {
            Student s = new Student();
            s.setEmail(email);
            s.setName("Performance Student");
            s.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890");
            return studentRepository.save(s);
        });

        // Warm up query
        studentRepository.findByEmail(email);

        // 100 buscas simultâneas pelo estudante usando o repositório com @EntityGraph
        BenchmarkResult result = runConcurrentBenchmark(100, 10, () -> {
            var opt = studentRepository.findByEmail(email);
            return opt.isPresent() && opt.get().getName().equals("Performance Student");
        });

        log.info("[BENCHMARK-STUDENT-ENTITYGRAPH] {}", result);
        assertEquals(0, result.errors);
        assertTrue(result.meanMs < 35.0, "EntityGraph single query deve responder em média em menos de 35ms");
    }

    @Test
    @DisplayName("Concorrência: Simulação de acessos concorrentes sem deadlock ou corrupção de estado")
    void testConcurrentAccessNoDeadlock() throws Exception {
        BenchmarkResult result = runConcurrentBenchmark(150, 15, () -> {
            int status = mockMvc.perform(get("/api/catalog/proficiency-levels"))
                .andReturn().getResponse().getStatus();
            return status == 200;
        });

        log.info("[BENCHMARK-CONCURRENCY-15-THREADS] {}", result);
        assertEquals(0, result.errors, "Nenhum erro de concorrência ou deadlock deve ocorrer");
        assertTrue(result.rps > 100, "Throughput deve superar 100 RPS no ambiente local");
    }
}
