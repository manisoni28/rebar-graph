FROM rebar-scanner-core:latest






COPY src/main/docker/app /app
COPY build/libs/*-all.jar /app/app.jar

ARG GIT_COMMIT=unknown
ARG GIT_REPO=unknwon
ARG CI_BUILD_URL=unknown

LABEL GIT_COMMIT=$GIT_COMMIT
LABEL GIT_REPO=$GIT_REPO
LABEL CI_BUILD_URL=$CI_BUILD_URL

RUN chmod +x /app/docker-init


CMD ["/app/docker-init"]
