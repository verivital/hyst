#!/bin/bash
set -e
# This script will download and install Hyst in /hyst and all relevant tools in /tools.
#
# To run this script without download, run the following command in a terminal:
# wget -q -O download.sh https://raw.githubusercontent.com/verivital/hyst/master/download_and_install_on_ubuntu_18_04.sh && bash download.sh
sudo apt-get update
sudo apt-get -qy install git
cd ~
rm -rf ./hyst-temp
git clone https://github.com/verivital/hyst hyst-temp --branch=master --recurse-submodules 
cd hyst-temp
sudo -H ./install_on_ubuntu_18_04.sh

sudo chown -R $USER "${HYST_PREFIX}/tools"
sudo chown -R $USER "${HYST_PREFIX}/hyst"
cd ..
rm -rf ./hyst-temp
