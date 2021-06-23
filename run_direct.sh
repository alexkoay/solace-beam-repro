#!/usr/bin/env bash

ROOT=$(realpath -s $(dirname ${BASH_SOURCE[0]})/..)

python3 -m pipeline.run \
    --runner DirectRunner
