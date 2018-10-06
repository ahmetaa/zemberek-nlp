#!/bin/bash

protoc --proto_path=./../proto   --php_out=./   --grpc_out=./ \
 --plugin=protoc-gen-grpc=/home/ahmetaa/projects/grpc/bins/opt/grpc_php_plugin \
 ./../proto/language_id.proto \
 ./../proto/simple_analysis.proto \
 ./../proto/preprocess.proto \
 ./../proto/normalization.proto


