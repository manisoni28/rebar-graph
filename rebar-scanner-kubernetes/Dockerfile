FROM rebar/scanner-core:latest


RUN curl -o /bin/kubectl  https://storage.googleapis.com/kubernetes-release/release/v1.13.2/bin/linux/amd64/kubectl && \
chmod +x /bin/kubectl 

COPY build/libs/rebar-scanner*.jar /app/app.jar


ARG GIT_COMMIT=unknown
ARG GIT_REPO=unknwon
ARG CI_BUILD_URL=unknown

LABEL GIT_COMMIT=$GIT_COMMIT
LABEL GIT_REPO=$GIT_REPO
LABEL CI_BUILD_URL=$CI_BUILD_URL

USER rebar
CMD ["/app/docker-init"]
