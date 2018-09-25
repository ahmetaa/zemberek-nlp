#!/bin/bash

protoc --proto_path=./../proto   --php_out=./   --grpc_out=./   --plugin=protoc-gen-grpc=/home/ahmetaa/projects/grpc/bins/opt/grpc_php_plugin   ./../proto/language_id.proto


