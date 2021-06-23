# Solace Beam in Python issue reproduction

This is a minimal working reproduction of code that causes the Solace Beam transform in Java to behave erratically.
This includes the Python setup for using the Java transform, although the issue manifests itself consistently with a Java-only pipeline with Beam SDK 2.25+ due to the new SplittableDoFn Read.

## System requirements

You'll need Python 3.7, Java 11 and Docker installed on the machine beforehand.

## Run once to install Python dependencies

```sh
pip3 install pipenv
pipenv install
```

Alternatively, you can use `pipenv install --system` to avoid having to use `pipenv run` or `pipenv shell` everywhere,
but it pollutes your global Python modules.
This would be okay for ad-hoc VMs, but not so much for a persistent workstation.

## Run to build a new JAR for the Java Beam expansion service

This is also run as part of the pipeline steps if the `*-all.jar` JAR is missing from `build/libs`

```sh
./gradlew shadowJar
```

## Run pipeline locally

```sh
pipenv run python3 -m pipeline.run
```

Alternatively, you can open a shell then run

```
pipenv shell
python3 -m pipeline run
```

## Run pipeline in Dataflow

For Dataflow, we need to build a Docker image beforehand for the Python Beam worker and push it to Cloud Repository before executing the pipeline.
The gist of the steps is captured in `run_dataflow.sh` but requires the missing variables to be filled in beforehand.

Once the missing values have been populated, run the following:

```sh
pipenv shell
bash ./run_dataflow.sh
```
