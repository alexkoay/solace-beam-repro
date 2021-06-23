import logging
from typing import Dict, List, NamedTuple, Optional, Tuple, Union

from apache_beam.typehints.decorators import with_output_types

from .expansion_service import default_expansion_service

from apache_beam.transforms.external import (
    ExternalTransform,
    JavaJarExpansionService,
    NamedTupleBasedPayloadBuilder,
)

ReadFromSolaceSchema = NamedTuple(
    "ReadFromSolaceSchema",
    [
        ("jcsmpProperties", Dict[str, str]),
        ("queues", List[str]),
        ("useSenderTimestamp", Optional[bool]),
        ("advanceTimeoutInMillis", Optional[int]),
        ("maxNumRecords", Optional[int]),
        ("maxReadTime", Optional[int]),
    ],
)


@with_output_types(Tuple[str, bytes])
class ReadFromSolace(ExternalTransform):
    URN = "beam:external:java:solaceio:read:v1"

    def __init__(
        self,
        host: str,
        queue: str,
        vpn_name: str = "default",
        *,
        username: Optional[str] = None,
        password: Optional[str] = None,
        jcsmpProperties: Optional[Dict[str, str]] = None,
        useSenderTimestamp: Optional[bool] = None,
        advanceTimeoutInMillis: Optional[int] = None,
        maxNumRecords: Optional[int] = None,
        maxReadTime: Optional[int] = None,
        expansion_service: Optional[Union[JavaJarExpansionService, str]] = None,
    ):
        props: Dict[str, str] = jcsmpProperties.copy() if jcsmpProperties is not None else dict()
        props["host"] = host
        props["vpn_name"] = vpn_name

        if username is None or password is None:
            import string
            import random

            # The Solace transform expects a non-empty value for the username and password
            # even though it supports connecting without authentication.
            # We use a random string to avoid reliance on any default values.

            log = logging.getLogger("pipeline.blocks.solaceio")
            log.info("No username/password received, defaulting to random letters instead.")
            props["username"] = "".join(random.choice(string.ascii_letters) for _ in range(8))
            props["password"] = "".join(random.choice(string.ascii_letters) for _ in range(8))

        else:
            props["username"] = username
            props["password"] = password

        payload = NamedTupleBasedPayloadBuilder(
            ReadFromSolaceSchema(
                jcsmpProperties=props,
                queues=[queue],
                useSenderTimestamp=useSenderTimestamp,
                advanceTimeoutInMillis=advanceTimeoutInMillis,
                maxNumRecords=maxNumRecords,
                maxReadTime=maxReadTime,
            )
        )

        super().__init__(
            self.URN,
            payload,
            expansion_service if expansion_service else default_expansion_service(),
        )
