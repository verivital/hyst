#!/bin/sh
set -e
echo "Notice: You can speed up this script if you run it as SKIP_MOST_TESTS=1 ./python_test_coverage.sh"
echo "Then, only a few integration tests will be run"
cd "$(dirname $0)"
COVERAGE=python2-coverage
# note that python2-coverage loads its settings from .coveragerc
COVERAGE_RUN="$COVERAGE run"
export NUM_THREADS=1 # workaround because multiprocessing.Pool is currently not supported by python2-coverage, and even causes crashes
export PYTHON_EXECUTABLE="$COVERAGE_RUN"
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
