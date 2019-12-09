#!/bin/sh
set -e
echo "Notice: You can speed up this script if you run it as SKIP_MOST_TESTS=1 ./python_test_coverage.sh"
echo "Then, only a few integration tests will be run"
PYTHON=${PYTHON:-3}
echo "Running for python version ${PYTHON}. Override this by running this script as PYTHON=x ./python_test_coverage.sh"
cd "$(dirname $0)"
COVERAGE=${COVERAGE:-python${PYTHON}-coverage}
# note that python2-coverage loads its settings from .coveragerc
COVERAGE_RUN="$COVERAGE run"
export PYTHON_COVERAGE=1 # switch on coverage measurement workarounds in the python scripts
export NUM_THREADS=1 # workaround because multiprocessing.Pool is currently not supported by python2-coverage, and even causes crashes
mkdir -p python_coverage
rm -r python_coverage/
$COVERAGE erase
$COVERAGE_RUN tests/integration/run_tests.py
$COVERAGE_RUN -m unittest discover -v
# Tests in hybridpy/ are not autodiscovered
$COVERAGE_RUN -m unittest discover -v -s hybridpy/
$COVERAGE combine
$COVERAGE report
$COVERAGE html
echo "A detailed report can be found in $(pwd)/python_coverage/index.html"
