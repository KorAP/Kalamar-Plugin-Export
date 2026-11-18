# Corretto 25.0.5.1.2 fixes CVE-2026-114938 (Java4Shell).
# Pin the patched build: the upstream tag omits the security patch suffix.
# Retain the Java SE APIs, TLS providers and locales used by the exporters.
ARG JAVA_MODULES=java.se,jdk.charsets,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.localedata,jdk.naming.dns,jdk.naming.rmi,jdk.security.auth,jdk.security.jgss,jdk.unsupported,jdk.zipfs,jdk.management,jdk.management.agent,jdk.httpserver,jdk.attach,jdk.jcmd
FROM amazoncorretto:25.0.5-alpine3.23@sha256:6198a628ef255ef0804ce8240303968d417eb30d6286f58857fea9ece3af1963 AS java-runtime
ARG JAVA_MODULES
RUN apk add --no-cache binutils && \
    jlink --add-modules "$JAVA_MODULES" \
    --strip-debug --compress=zip-6 --no-header-files --no-man-pages \
    --output /opt/java/runtime && \
    rm /opt/java/runtime/lib/security/cacerts && \
    cp -L "$JAVA_HOME/lib/security/cacerts" /opt/java/runtime/lib/security/cacerts

FROM java-runtime AS builder

# Copy repository respecting .dockerignore
COPY . /export

WORKDIR /export

RUN apk update && \
    apk add --no-cache maven

# Build the project using local source code
RUN mvn -Djvm=/opt/java/runtime/bin/java \
    -Dexport.api.scheme=http -Dexport.api.host=kustvakt -Dexport.api.port=8089 \
    clean package

# Package
RUN find target/KalamarExportPlugin-*.jar -exec mv {} KalamarExportPlugin.jar ';'

# Clean up Maven cache
RUN rm -rf ~/.m2/repository

# Use glibc for the runtime: Alpine-based Java images have caused native
# compatibility problems on older Docker hosts, including CentOS 7.
FROM amazoncorretto:25.0.5-al2023@sha256:8b82be5adeabd374f5438e90635a120868e07ea8e85be6b691eb1c994d53f1e2 AS java-runtime-glibc
ARG JAVA_MODULES
RUN yum install -y binutils && \
    jlink --add-modules "$JAVA_MODULES" --strip-debug --compress=zip-6 \
    --no-header-files --no-man-pages --output /opt/java/runtime && \
    rm /opt/java/runtime/lib/security/cacerts && \
    cp -L "$JAVA_HOME/lib/security/cacerts" /opt/java/runtime/lib/security/cacerts

FROM public.ecr.aws/amazonlinux/amazonlinux:2023-minimal@sha256:c64f3b683d01c1c874831adc6a7b5a2fd6f0f9affd58bc638939a9f46cc03543
ENV KALAMAR_EXPORT_SERVER_HOST=0.0.0.0
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=java-runtime-glibc /opt/java/runtime /opt/java/openjdk

# Preserve the Alpine image's IDs for existing bind-mounted export files.
RUN microdnf install -y --setopt=install_weak_deps=0 shadow-utils libstdc++ zlib ca-certificates fontconfig freetype && \
    microdnf clean all && \
    groupadd --system --gid 101 korap && \
    useradd --system --uid 100 -g korap export && \
    mkdir export && \
    chown -R export:korap /export

WORKDIR /export

COPY --from=builder /export/KalamarExportPlugin.jar /export/

USER export

EXPOSE 7777

ENTRYPOINT [ "java", "-jar" ]

CMD [ "KalamarExportPlugin.jar" ]

# docker build -f Dockerfile -t korap/kalamar-plugin-export:{nr} .
