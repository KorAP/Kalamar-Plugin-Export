# Use alpine linux as base image
FROM eclipse-temurin:22-jdk-alpine AS builder

# Copy repository respecting .dockerignore
COPY . /export

WORKDIR /export

RUN apk update && \
    apk add --no-cache maven

# Build the project using local source code
RUN mvn clean package

# Package
RUN find target/KalamarExportPlugin-*.jar -exec mv {} KalamarExportPlugin.jar ';'

# Clean up Maven cache
RUN rm -rf ~/.m2/repository

FROM eclipse-temurin:22-jre-alpine

RUN addgroup -S korap && \
    adduser -S export -G korap && \
    mkdir export && \
    chown -R export:korap /export

WORKDIR /export

COPY --from=builder /export/KalamarExportPlugin.jar /export/

USER export

EXPOSE 7777

ENTRYPOINT [ "java", "-jar" ]

CMD [ "KalamarExportPlugin.jar" ]

# docker build -f Dockerfile -t korap/kalamar-plugin-export:{nr} .
