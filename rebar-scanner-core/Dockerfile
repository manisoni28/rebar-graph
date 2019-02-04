FROM openjdk:8-slim-stretch

RUN echo && \
apt-get -y update && \
apt-get -y install curl sudo && \
useradd -m -d /rebar -s /bin/bash rebar


COPY src/main/docker/app /app
RUN chmod +x /app/docker-init

ARG GIT_COMMIT=unknown
ARG GIT_REPO=unknwon
ARG CI_BUILD_URL=unknown

LABEL GIT_COMMIT=$GIT_COMMIT
LABEL GIT_REPO=$GIT_REPO
LABEL CI_BUILD_URL=$CI_BUILD_URL
LABEL maintainer="robschoening@gmail.com"

CMD ["/app/docker-init"]
