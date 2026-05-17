FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace

COPY . .

RUN apt-get -y update; apt-get -y install curl

RUN ./mill services.sourcing.assembly

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=builder /workspace/out/services/sourcing/assembly.dest/out.jar /app/sourcing.jar

CMD ["java", "-jar", "/app/sourcing.jar"]
