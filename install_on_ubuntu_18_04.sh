#!/bin/bash

# USAGE:
# - Install an Ubuntu 18.04 VM
# - Unpack the Hyst repository somewhere
# - Open a terminal in the directory containing this file
# - Run this script as root (or with sudo: sudo ./install_ubuntu_18_04.sh)
# -> Hyst can be found in the current directory (or as a symlink in /opt/hyst), and the tools at /opt/tools.

# HOW IT WORKS:
#  Just like sausage, it tastes good but you rather don't want to know how it is made ;-)
#  We parse the Dockerfile to get a shell script and then run it. Yes, that's ugly.


# exit if anything fails
set -e
set -o pipefail
shopt -s failglob

if [[ ! -e ./dockerfile-to-shell-script ]]; then
    echo "Cannot find the dockerfile-to-shell-script directory. Make sure you are running this script from the current directory. Make sure that the git submodule is checked out -- try 'git submodule --update --init'."
    exit 1
fi

if [[ $EUID -ne 0 ]]; then
   echo "This script must be run as root or with sudo" 
   exit 1
fi

SCRIPT_PATH="$(readlink -f "$0")"
SCRIPT_DIR="$(dirname "$SCRIPT_PATH")"

# where should hyst and the tools be installed? Default: /opt
#HYST_PREFIX=${HYST_PREFIX:-/opt}
# TODO - this needs extra support in the Dockerfile.
HYST_PREFIX="/"

echo "This script will install hyst at ${HYST_PREFIX}/hyst, and install its dependencies at ${HYST_PREFIX}/tools."

echo "DO NOT RUN THIS DIRECTLY ON YOUR NORMAL COMPUTER, ONLY IN AN EXTRA VIRTUAL MACHINE,"
echo "because some of the script's actions will permanently change configuration, uninstall packages et cetera."

echo "CAUTION: all existing content in the the ${HYST_PREFIX}/tools and ${HYST_PREFIX}/hyst folders will be removed."

if tty -s; then
    # ask for confirmation if interactive session
    echo "Press Ctrl-C to exit, Enter to continue."
    read
fi

mkdir -p "${HYST_PREFIX}"
rm -rf "${HYST_PREFIX}/tools" "${HYST_PREFIX}/hyst"

./dockerfile-to-shell-script/docker_to_sh.sh # converts Dockerfile to Dockerfile.sh
./Dockerfile.sh

# save environment variables to file
grep "export " Dockerfile.sh | sudo sh -c "cat > ${HYST_PREFIX}/hyst_environment"

# automatically load environment variables on login
echo "source '${HYST_PREFIX}/hyst_environment'" > /etc/profile.d/99-hyst.sh

echo "You need to log in and out again to load the required environment variables. Or run the following command:"
echo "source '${HYST_PREFIX}/hyst_environment'"
echo ""
echo "The tools are then available on the command line (hyst, spaceex, ...)."
