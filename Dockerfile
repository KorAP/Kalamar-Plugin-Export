# Use alpine linux as base image
FROM openjdk:19-alpine as builder

# Copy repository respecting .dockerignore
COPY . /export

WORKDIR /export

RUN apk update && \
    apk add --no-cache git \
            maven

RUN git config --global user.email "korap+docker@ids-mannheim.de" && \
    git config --global user.name "Docker"

# Install Kalamar-Plugin-Export
RUN mkdir built && \
    git clone https://github.com/KorAP/Kalamar-Plugin-Export.git Kalamar-Plugin-Export && \
    cd Kalamar-Plugin-Export && \
    git checkout master && \
    mvn clean package

# Package
RUN cd Kalamar-Plugin-Export && \
    find target/KalamarExportPlugin-*.jar -exec mv {} ../built/KalamarExportPlugin.jar ';'

RUN apk del git \
            maven

RUN cd ${M2_HOME} && rm -r .m2

FROM openjdk:19-alpine

RUN addgroup -S korap && \
    adduser -S export -G korap && \
    mkdir export && \
    chown -R export.korap /export

WORKDIR /export

COPY --from=builder /export/built/KalamarExportPlugin.jar /export/

USER export

EXPOSE 7777

ENTRYPOINT [ "java", "-jar" ]

CMD [ "KalamarExportPlugin.jar" ]


# docker build -f Dockerfile -t korap/kalamar-plugin-export:{nr} .
