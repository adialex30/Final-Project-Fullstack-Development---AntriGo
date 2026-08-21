    FROM maven:3.9-eclipse-temurin-17 AS build
    WORKDIR /app
    COPY pom.xml .
    RUN mvn -B dependency:go-offline
    COPY src ./src
    RUN mvn -B clean package -DskipTests

    FROM eclipse-temurin:17-jre-jammy
    WORKDIR /app
    RUN apt-get update \
        && apt-get install -y --no-install-recommends curl \
        && rm -rf /var/lib/apt/lists/* \
        && useradd -ms /bin/bash spring
    COPY --from=build /app/target/*.jar app.jar
    
    # -XX:MaxRAMPercentage=60      batas heap 60% dari RAM container (turun dari 75%),
    #                              sisakan lebih banyak ruang untuk metaspace & native memory
    # -XX:+UseSerialGC             GC paling ringan untuk heap kecil/single-core, dipertahankan
    # -XX:TieredStopAtLevel=1      pakai C1 JIT compiler saja (skip C2) -> startup lebih cepat,
    #                              CPU lebih hemat, trade-off throughput puncak yang tidak
    #                              relevan untuk API request kecil bertraffic rendah
    # -Xss512k                     thread stack diperkecil dari default 1MB, masih generus
    #                              untuk kedalaman call stack Spring/Hibernate biasa
    ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -Duser.timezone=Asia/Jakarta"
    
    EXPOSE 8080
    USER spring
    ENTRYPOINT ["java", "-jar", "app.jar"]