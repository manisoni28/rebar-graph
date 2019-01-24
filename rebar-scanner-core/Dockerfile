FROM openjdk:8-alpine


RUN apk update && \
apk add curl bash  && \
	rm /var/cache/apk/* && \
	adduser -D -h /rebar rebar

COPY src/main/docker/app /app
RUN chmod +x /app/docker-init

ARG GIT_COMMIT=unknown
ARG GIT_REPO=unknwon
ARG CI_BUILD_URL=unknown

LABEL GIT_COMMIT=$GIT_COMMIT
LABEL GIT_REPO=$GIT_REPO
LABEL CI_BUILD_URL=$CI_BUILD_URL

CMD ["/app/docker-init"]
