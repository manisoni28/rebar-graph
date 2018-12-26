FROM openjdk:8-alpine



RUN apk update && \
apk add curl bash  && \
curl -o /bin/kubectl  https://storage.googleapis.com/kubernetes-release/release/v1.12.0/bin/linux/amd64/kubectl && \
chmod +x /bin/kubectl &&  \
	apk -Uuv add groff less python py-pip && \
	pip install awscli && \
	apk --purge -v del py-pip && \
	rm /var/cache/apk/*




COPY src/main/docker/app /app
COPY build/libs/rebar-graph-aws-all.jar /app/app.jar


RUN chmod +x /app/docker-init


ARG GIT_COMMIT=unknown
ARG GIT_REPO=unknwon
LABEL GIT_COMMIT=$GIT_COMMIT
LABEL GIT_REPO=$GIT_REPO

CMD ["/app/docker-init"]
