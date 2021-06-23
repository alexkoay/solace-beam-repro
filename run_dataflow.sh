#!/usr/bin/env bash

export PROJECT=
export REPO=repro
export TAG=$(date +"%Y%m%d%H%M%S")
export REGISTRY_HOST=asia.gcr.io
export IMAGE_URI=$REGISTRY_HOST/$PROJECT/$REPO:$TAG
export REGION=
export ZONE=
export GCS_LOC=gs://alex-dev

docker build -t $IMAGE_URI .
docker push $IMAGE_URI

python3 -m pipeline.run \
    --runner DataflowRunner \
    --project $PROJECT \
    --region $REGION \
    --zone $ZONE \
    --temp_location=$GCS_LOC/temp \
    --staging_location=$GCS_LOC/staging \
    --experiments=use_runner_v2 \
    --worker_harness_container_image $IMAGE_URI
