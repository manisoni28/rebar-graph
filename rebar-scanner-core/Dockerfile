FROM openjdk:8-alpine


RUN apk update && \
apk add curl bash  && \
curl -o /bin/kubectl  https://storage.googleapis.com/kubernetes-release/release/v1.13.2/bin/linux/amd64/kubectl && \
chmod +x /bin/kubectl &&  \
	apk -Uuv add groff less python py-pip && \
	pip install awscli && \
	apk --purge -v del py-pip && \
	rm /var/cache/apk/* && \
	adduser -D -h /rebar rebar
