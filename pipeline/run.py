import logging
import sys

import apache_beam as beam

from apache_beam.options.pipeline_options import PipelineOptions

from pipeline import solace

logging.root.setLevel(logging.WARNING)

opts = PipelineOptions(sys.argv[1:], streaming=True)
p = beam.Pipeline(options=opts)

stages = [
    solace.ReadFromSolace(
        host="localhost",
        queue="queue",
        vpn_name="default",
        username="default",
        password="default",
    ),
    beam.combiners.Count.PerKey()
]

current = p
for i, stage in enumerate(stages):
    current |= stage

    # print output
    current | f"print#{i}" >> beam.Map(print)
    print("added", stage)

p.run()
