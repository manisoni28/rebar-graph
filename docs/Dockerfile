FROM alpine


RUN echo start && \
apk add python3 py-pip && \
pip install mkdocs pygments mkdocs-material

WORKDIR /docs
EXPOSE 8000

