# Build from the repository root:  docker build -f bff/Dockerfile -t deco-socio-bff .
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon -PbffOnly=true :bff:installDist

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/bff/build/install/bff /app
ENV PORT=8080 SFMC_MODE=mock
EXPOSE 8080
# Run as a non-root user.
RUN useradd --system --uid 10001 bff
USER bff
ENTRYPOINT ["/app/bin/bff"]
