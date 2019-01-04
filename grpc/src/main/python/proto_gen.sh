#!/bin/bash
mkdir zemberek-grpc
python -m grpc_tools.protoc -I../proto --python_out=zemberek-grpc --grpc_python_out=zemberek-grpc\
 ./../proto/language_id.proto \
 ./../proto/morphology.proto \
 ./../proto/preprocess.proto \
 ./../proto/normalization.proto

touch zemberek-grpc/__init__.py