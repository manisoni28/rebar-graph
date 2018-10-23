FROM openjdk:8-alpine

ARG GIT_COMMIT=unknown
ARG GIT_REPO=unknwon
LABEL GIT_COMMIT=$GIT_COMMIT
LABEL GIT_REPO=$GIT_REPO

RUN apk update

RUN apk add curl bash  && \
curl -o /bin/kubectl  https://storage.googleapis.com/kubernetes-release/release/v1.12.0/bin/linux/amd64/kubectl && \
chmod +x /bin/kubectl 




COPY src/main/docker/app /app
COPY build/libs/rebar-graph-kubernetes-capsule.jar /app/app.jar


RUN chmod +x /app/docker-init

CMD ["/app/docker-init"]
