#!/bin/bash

docker build . -t mkdocs

docker run -v `pwd`:/docs -p 8000:8000 -it mkdocs mkdocs serve --dev-addr=0.0.0.0:8000
