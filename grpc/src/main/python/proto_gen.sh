#!/bin/bash

python -m grpc_tools.protoc -I../proto --python_out=. --grpc_python_out=.\
 ./../proto/language_id.proto \
 ./../proto/simple_analysis.proto \
 ./../proto/preprocess.proto \
 ./../proto/normalization.proto