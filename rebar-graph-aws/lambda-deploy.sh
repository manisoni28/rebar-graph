#!/bin/bash

ZIP_FILE=./build/distributions/lambda.zip
SHA=$(shasum $ZIP_FILE | awk '{ print $1 }')

S3_KEY=artifacts/rebar-graph-aws/${SHA}/lambda.zip
S3_BUCKET="rebar-artifacts"
S3_TARGET=s3://${S3_BUCKET}/artifacts/rebar-graph-aws/${SHA}/lambda.zip

echo checking $S3_TARGET
aws s3 ls $S3_TARGET

if [ $? -gt 0 ]; then
    aws s3 cp $ZIP_FILE $S3_TARGET || exit 1
fi

aws lambda update-function-code --function-name foobar --s3-bucket rebar-artifacts --s3-key ${S3_KEY}