FROM apache/beam_python3.7_sdk:2.29.0

RUN mkdir /repro
WORKDIR /repro

RUN pip install pipenv
COPY Pipfile* /repro/
RUN pipenv install --system --deploy

COPY / /repro/
