#!/bin/bash


rm -rf gh-pages site

REMOTE_URL=`git remote -v | grep push | grep origin | awk '{ print $2 }'`

git clone -b gh-pages $REMOTE_URL gh-pages

docker build . -t mkdocs

docker run -v `pwd`:/docs -p 8000:8000 -it mkdocs mkdocs build

cp -r site/ gh-pages

cd gh-pages
git add .
git commit -a -m "update"


git status

#git push -f