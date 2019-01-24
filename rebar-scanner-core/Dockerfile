FROM openjdk:8-alpine


RUN apk update && \
apk add curl bash  && \
	rm /var/cache/apk/* && \
	adduser -D -h /rebar rebar

COPY src/main/docker/app /app
RUN chmod +x /app/docker-init

CMD ["/app/docker-init"]
