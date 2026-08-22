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
    
    # -XX:MaxRAMPercentage=40      batas heap 40% dari RAM container (turun dari 60%) — di container
    #                              512MB (Railway Free), stack Spring+Hibernate+Security+OpenAPI
    #                              butuh metaspace/native jauh lebih besar dari sisa 25% yang lama
    # -XX:MaxMetaspaceSize=160m    metaspace TANPA batas ini bisa tumbuh sampai habisin RAM container
    #                              (beda dari heap OOM biasa — ini yang paling sering bikin container
    #                              di-kill kernel walau heap Java sendiri masih kelihatan aman)
    # -XX:ReservedCodeCacheSize=48m  cocok untuk TieredStopAtLevel=1 (cuma C1), jauh di bawah default 240m
    # -XX:MaxDirectMemorySize=32m  cap buffer NIO Tomcat, gak ikut ke-hitung MaxRAMPercentage otomatis
    # -XX:+UseSerialGC             GC paling ringan untuk heap kecil/single-core, dipertahankan
    # -XX:TieredStopAtLevel=1      pakai C1 JIT compiler saja (skip C2) -> startup lebih cepat,
    #                              CPU lebih hemat, trade-off throughput puncak yang tidak
    #                              relevan untuk API request kecil bertraffic rendah
    # -Xss384k                     thread stack diperkecil dari 512k, masih cukup untuk kedalaman
    #                              call stack Spring/Hibernate biasa
    # -XX:+ExitOnOutOfMemoryError  kalau beneran OOM di level Java, JVM mati bersih & cepat, biar
    #                              Railway restart cepat -- bukan nyangkut GC-thrashing diam-diam
    ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=40.0 -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss384k -XX:+ExitOnOutOfMemoryError -Duser.timezone=Asia/Jakarta"
    
    EXPOSE 8080
    USER spring
    ENTRYPOINT ["java", "-jar", "app.jar"]