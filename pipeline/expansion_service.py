import glob
import logging
import os.path
import subprocess
from typing import Optional

from apache_beam.transforms.external import JavaJarExpansionService

ROOT_DIR = os.path.abspath(__file__ + "/../../")
BUILD_GLOB = "build/libs/*-all.jar"


def find_jars():
    return glob.glob(os.path.join(ROOT_DIR, BUILD_GLOB))


def build_jar():
    subprocess.run([os.path.join(ROOT_DIR, "gradlew"), "-p", ROOT_DIR, "shadowJar"])
    return find_jars()[0]


def default_expansion_service(jar_path: Optional[str] = None):
    log = logging.getLogger("utils.expansion")
    if jar_path is None:
        jars = find_jars()
        if not jars:
            log.info("Building JAR from Gradle project")
            jar_path = build_jar()
        else:
            jar_path = jars[0]
        log.info("Using JAR found in project: %s", jars[0])
    else:
        log.info("Using specified JAR: %s", jar_path)
    return JavaJarExpansionService(jar_path)
