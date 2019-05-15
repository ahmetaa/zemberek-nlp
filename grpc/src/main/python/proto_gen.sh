#!/bin/bash
mkdir zemberek_grpc
python3 -m grpc_tools.protoc -I../proto --python_out=zemberek_grpc --grpc_python_out=zemberek_grpc\
 ./../proto/language_id.proto \
 ./../proto/morphology.proto \
 ./../proto/preprocess.proto \
 ./../proto/normalization.proto

touch zemberek_grpc/__init__.py
