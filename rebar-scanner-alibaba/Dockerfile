FROM rebar-scanner-core:latest


RUN apk update

RUN apk add curl bash  && \
curl -o /bin/kubectl  https://storage.googleapis.com/kubernetes-release/release/v1.12.0/bin/linux/amd64/kubectl && \
chmod +x /bin/kubectl 




COPY src/main/docker/app /app
COPY build/libs/*-all.jar /app/app.jar


RUN chmod +x /app/docker-init

ARG GIT_COMMIT=unknown
ARG GIT_REPO=unknwon
ARG CI_BUILD_URL=unknown

LABEL GIT_COMMIT=$GIT_COMMIT
LABEL GIT_REPO=$GIT_REPO
LABEL CI_BUILD_URL=$CI_BUILD_URL

CMD ["/app/docker-init"]
