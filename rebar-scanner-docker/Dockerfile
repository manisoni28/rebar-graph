FROM rebar/scanner-core:latest


COPY build/libs/*-all.jar /app/app.jar

ARG GIT_COMMIT=unknown
ARG GIT_REPO=unknwon
ARG CI_BUILD_URL=unknown

LABEL GIT_COMMIT=$GIT_COMMIT
LABEL GIT_REPO=$GIT_REPO
LABEL CI_BUILD_URL=$CI_BUILD_URL


USER rebar
CMD ["/app/docker-init"]
