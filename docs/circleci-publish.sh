#!/bin/bash


env | grep CIRCLE

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd $SCRIPT_DIR

SOURCE_SHA1=${CIRCLE_SHA1-"unknown"}
rm -rf ./tmp-clone ./site

REMOTE_URL=git@github.com:rebar-cloud/rebar-cloud.git

git clone $REMOTE_URL tmp-clone || exit 99

mkdocs build || exit 99

cp -r site/ tmp-clone

cd tmp-clone

git add .
git commit -a -m "docs built from $SOURCE_SHA1"

git push 

exit 0

