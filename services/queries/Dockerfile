FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace

COPY . .

RUN apt-get -y update; apt-get -y install curl

RUN ./mill services.queries.assembly

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=builder /workspace/out/services/queries/assembly.dest/out.jar /app/queries.jar

EXPOSE 9000

CMD ["java", "-jar", "/app/queries.jar"]
