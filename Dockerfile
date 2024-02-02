# hadolint global ignore=DL3006
ARG BASE_IMAGE="alpine:3.19.1"
ARG BASE_DIGEST_AMD64="sha256:6457d53fb065d6f250e1504b9bc42d5b6c65941d57532c072d929dd0628977d0"
ARG BASE_DIGEST_ARM64="sha256:a0264d60f80df12bc1e6dd98bae6c43debe6667c0ba482711f0d806493467a46"

# Prepare tasklist Distribution
FROM ${BASE_IMAGE} as prepare

WORKDIR /tmp/tasklist

# download tasklist
COPY distro/target/camunda-tasklist-*.tar.gz tasklist.tar.gz
RUN tar xzvf tasklist.tar.gz --strip 1 && \
    rm tasklist.tar.gz

### AMD64 base image ###
# BASE_DIGEST_AMD64 is defined at the top of the Dockerfile
# hadolint ignore=DL3006
FROM ${BASE_IMAGE}@${BASE_DIGEST_AMD64} as base-amd64
ARG BASE_DIGEST_AMD64
ARG BASE_DIGEST="${BASE_DIGEST_AMD64}"

# Install Tini for amd64
RUN apk update && apk add --no-cache tini

### ARM64 base image ##
# BASE_DIGEST_ARM64 is defined at the top of the Dockerfile
# hadolint ignore=DL3006
FROM ${BASE_IMAGE}@${BASE_DIGEST_ARM64} as base-arm64
ARG BASE_DIGEST_ARM64
ARG BASE_DIGEST="${BASE_DIGEST_ARM64}"

# Install Tini for arm64
RUN apk update && apk add --no-cache tini

### Application Image ###
# TARGETARCH is provided by buildkit
# https://docs.docker.com/engine/reference/builder/#automatic-platform-args-in-the-global-scope
# hadolint ignore=DL3006

FROM base-${TARGETARCH} as app
# leave unset to use the default value at the top of the file
ARG BASE_IMAGE
ARG BASE_DIGEST
ARG VERSION=""
ARG DATE=""
ARG REVISION=""

# OCI labels: https://github.com/opencontainers/image-spec/blob/main/annotations.md
LABEL org.opencontainers.image.base.name="docker.io/library/${BASE_IMAGE}"
LABEL org.opencontainers.image.base.digest="${BASE_DIGEST}"
LABEL org.opencontainers.image.created="${DATE}"
LABEL org.opencontainers.image.authors="hto@camunda.com"
LABEL org.opencontainers.image.url="https://camunda.com/platform/tasklist/"
LABEL org.opencontainers.image.documentation="https://docs.camunda.io/docs/self-managed/tasklist-deployment/install-and-start/"
LABEL org.opencontainers.image.source="https://github.com/camunda/tasklist"
LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.revision="${REVISION}"
LABEL org.opencontainers.image.vendor="Camunda Services GmbH"
LABEL org.opencontainers.image.licenses="Proprietary"
LABEL org.opencontainers.image.title="Camunda Tasklist"
LABEL org.opencontainers.image.description="Tasklist is a ready-to-use application to rapidly implement business processes alongside user tasks in Zeebe"

# OpenShift labels: https://docs.openshift.com/container-platform/4.10/openshift_images/create-images.html#defining-image-metadata
LABEL io.openshift.tags="bpmn,tasklist,camunda"
LABEL io.openshift.wants="zeebe,elasticsearch"
LABEL io.openshift.non-scalable="false"
LABEL io.openshift.min-memory="1Gi"
LABEL io.openshift.min-cpu="1"
LABEL io.k8s.description="Tasklist is a ready-to-use application to rapidly implement business processes alongside user tasks in Zeebe"

EXPOSE 8080

RUN apk update && apk upgrade && \
    apk add --no-cache bash openjdk17-jre tzdata

WORKDIR /usr/local/tasklist
VOLUME /tmp

COPY --from=prepare /tmp/tasklist /usr/local/tasklist

RUN addgroup --gid 1001 camunda && adduser -D -h /usr/local/tasklist -G camunda -u 1001 camunda
USER 1001:1001

ENTRYPOINT ["/sbin/tini", "--", "/usr/local/tasklist/bin/tasklist"]
