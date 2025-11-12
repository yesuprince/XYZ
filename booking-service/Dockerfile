# ---------- Build Stage ----------
FROM eclipse-temurin:23-jdk AS build
WORKDIR /app

# Copy only what Maven needs for dependency resolution
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ---------- Runtime Stage ----------
FROM eclipse-temurin:23-jre
WORKDIR /app

ARG CERT_FILE=/certs/myCA.crt
COPY ${CERT_FILE} /tmp/myCA.crt
RUN if [ -f /tmp/myCA.crt ]; then \
      echo "Importing custom certificate into JDK truststore..." && \
      keytool -importcert -noprompt \
        -storepass changeit \
        -keystore $JAVA_HOME/lib/security/cacerts \
        -alias customCA \
        -file /tmp/myCA.crt ; \
    else \
      echo "No custom certificate provided, skipping import."; \
    fi

# Copy application JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose app port
EXPOSE 8080

# Start Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]