# Build Stage
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

# Otimização de cache do Docker: baixa dependências antes de copiar código-fonte
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copia código e realiza empacotamento
COPY src src
RUN ./mvnw -DskipTests package -B

# Runtime Stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Instala curl para health checks nativos do Docker/K8s
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Cria usuário sem privilégios administrativos (Princípio do Menor Privilégio)
RUN groupadd -g 10001 appgroup \
    && useradd -u 10001 -g appgroup --create-home --shell /bin/bash appuser \
    && mkdir -p /app/uploads/profile-pictures \
    && chown -R appuser:appgroup /app

COPY --from=build --chown=appuser:appgroup /workspace/target/*.jar /app/app.jar

USER appuser

EXPOSE 8080

# Flags JVM para limites de memória em container, encoding UTF-8 e entropia rápida
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Healthcheck nativo consultando o Actuator
HEALTHCHECK --interval=20s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=docker"]
