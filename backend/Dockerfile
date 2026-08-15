## ---- build stage ----
#FROM maven:3.9-eclipse-temurin-17 AS build
#WORKDIR /app
#COPY pom.xml .
#RUN mvn -B dependency:go-offline
#COPY src ./src
#RUN mvn -B clean package -DskipTests
#
## ---- run stage ----
#FROM eclipse-temurin:17-jre-jammy
#WORKDIR /app
#RUN apt-get update \
#    && apt-get install -y --no-install-recommends curl \
#    && rm -rf /var/lib/apt/lists/* \
#    && useradd -ms /bin/bash spring
#COPY --from=build /app/target/antrigo-backend.jar app.jar
#EXPOSE 8081
#USER spring
#ENTRYPOINT ["java", "-jar", "app.jar"]


# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- run stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -ms /bin/bash spring
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
USER spring
ENTRYPOINT ["java", "-jar", "app.jar"]