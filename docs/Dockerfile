FROM alpine


RUN echo start && \
apk add python3 py-pip && \
pip install mkdocs pygments mkdocs-material

WORKDIR /docs
EXPOSE 8000

CMD [ "mkdocs", "serve", "--dev-addr=0.0.0.0:8000" ]