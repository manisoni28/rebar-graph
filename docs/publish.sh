#!/bin/bash


rm -rf tmp-clone site

REMOTE_URL=git@github.com:rebar-cloud/rebar-cloud.git

git clone $REMOTE_URL tmp-clone

docker build . -t mkdocs

docker run -v `pwd`:/docs -p 8000:8000 -it mkdocs mkdocs build

cp -r site/ tmp-clone

cd tmp-clone
git add .
git commit -a -m "update"


git status

git push 