# Dockerfile building a docker container with Hyst and all related tools
# This file is also helpful as a machine-readable and automatically tested instruction on how to build Hyst.

FROM ubuntu:18.04
# apt should not ask any questions:
ENV DEBIAN_FRONTEND=noninteractive

# add the following line to use a mirror which is nearer to you than the default archive.ubuntu.com (example: ftp.fau.de):
# RUN sed 's@archive.ubuntu.com@ftp.fau.de@' -i /etc/apt/sources.list

##################
# Install Hyst dependencies
##################
RUN apt-get update && apt-get -qy install ant python2.7 python-scipy python-matplotlib git libglpk-dev build-essential python-cvxopt python-sympy gimp

# Bug in sympy < 1.2: "TypeError: argument is not an mpz" (probably https://github.com/sympy/sympy/issues/7457, was fixed Nov 2017)
# -> we use sympy 1.2
RUN apt-get -qy install python-pip python-sympy- && pip install sympy==1.2

##################
# Install Hylaa
##################
# branch or tag of hylaa to use
ENV HYLAA_VERSION v1.1
ENV PYTHONPATH=$PYTHONPATH:/tools/hylaa:/tools/hylaa/hylaa/
RUN mkdir -p /tools/hylaa && git clone https://github.com/stanleybak/hylaa /tools/hylaa --branch $HYLAA_VERSION  --depth 1
RUN cd /tools/hylaa/hylaa/glpk_interface && make
RUN ls -l /tools/hylaa
RUN echo $HOME
# BUG (TODO report???)  The hylaa unittests cannot be run noninteractive because matplotlib fails if no X server is running. Workaround by changing the default backend from TkAgg (interactive) to Agg (noninteractive).
RUN sed -i 's/^backend *: *TkAgg$/backend: Agg/i' /etc/matplotlibrc
RUN cd /tools/hylaa/tests && python -m unittest discover

# TODO: performance warning because numpy is not compiled with OpenBLAS

##################
# Install flowstar
##################
# Configuration:
# FLOWSTAR_VERSION: version of flowstar,
# FLOWSTAR_FILE_SHA512SUM: sha512sum hash of the .tar.gz download archive of flowstar.
# to disable the hash check, change the line to:
# ENV FLOWSTAR_FILE_SHA512SUM ' '

ENV FLOWSTAR_VERSION 2.0.0
ENV FLOWSTAR_FILE_SHA512SUM '641179b88a2eb965266f3ec0d8adca6726d5b2a172a5686ae59c1b8fc6bb9dc662ef67d95eb8c158175fd1f411e5db7355a83e5dd12fd4d8fb196e27d4988f79'

# We can't use 2.1.0 yet due to this bug: https://github.com/verivital/hyst/issues/44
# ENV FLOWSTAR_VERSION 2.1.0
# ENV FLOWSTAR_FILE_SHA512SUM 'd5243f3bbcdd6bffcaf2f1ae8559278f62567877021981e4443cd90fbf2918e0acb317a2d27724bc81d3a0e38ad7f7d48c59d680be1dd5345e80d2234dd3fe3b'


RUN mkdir -p /tools/flowstar
WORKDIR /tools/flowstar
RUN apt-get install -qy curl flex bison libgmp-dev libmpfr-dev libgsl-dev gnuplot 
RUN curl -fL https://www.cs.colorado.edu/~xich8622/src/flowstar-${FLOWSTAR_VERSION}.tar.gz > flowstar.tar.gz
# print and check hash
RUN sha512sum flowstar.tar.gz | tee flowstar.sha512sum && grep -q "${FLOWSTAR_FILE_SHA512SUM}" flowstar.sha512sum
RUN tar xzf flowstar.tar.gz
WORKDIR /tools/flowstar/flowstar-${FLOWSTAR_VERSION}/
RUN make
ENV PATH=$PATH:/tools/flowstar/flowstar-${FLOWSTAR_VERSION}/


##################
# Install SpaceEx
##################
# SHA512 hash of the downloaded file (set hash to ' ' to disable hash checking)
ENV SPACEEX_FILE_SHA512SUM '30eab345ca8cbc5722df38e7ed6009c728e197184eca7c49558eeb73055ef340177315aa72f0809acca1dbde4a969779729ea1cb2529ed0b6a3e221ffd0c82b3'
RUN mkdir -p /tools/spaceex
WORKDIR /tools/spaceex
# We use the SpaceEx 64bit executable file.
# TODO: SpaceEx doesn't provide a publicly available download URL, you need to fill out the registration form first :-( -> As a workaround, we use the following Github link.
RUN curl -fL https://github.com/MaxGaukler/hyst/raw/1b50946bc01051626ff0fc3c90f5d5a6e625a89a/spaceex_exe-0.9.8f.tar.gz > spaceex.tar.gz
# print and check hash
RUN sha512sum spaceex.tar.gz | tee spaceex.sha512sum && grep -q "${SPACEEX_FILE_SHA512SUM}" spaceex.sha512sum
RUN tar xzf ./spaceex.tar.gz
RUN apt-get install -qy plotutils
ENV PATH=$PATH:/tools/spaceex/spaceex_exe/
RUN spaceex --version

##################
# Install dReach (included in dReal3)
##################
# version and SHA512 hash (set hash to ' ' to disable hash checking)
# see https://github.com/dreal/dreal3/releases for available versions
# It seems that dReal4 does no longer include dReach, so we're stuck wich dReal3.
ENV DREAL_VERSION 3.16.06.02
ENV DREAL_FILE_SHA512SUM '199c02d90d3d448dff6b9d2d1b99257d4ae4efcf22fa4d66d30eeb0cb6215b06ff8824c4256bf1b89ebaf01b872655ab3105d298c3db0a28d6c0c71a24fa0712'


WORKDIR /tools/dreach

RUN curl -fL https://github.com/dreal/dreal3/releases/download/v3.16.06.02/dReal-3.16.06.02-linux.tar.gz > dreach.tar.gz
# print and check hash
RUN sha512sum dreach.tar.gz | tee dreach.sha512sum && grep -q "${DREAL_FILE_SHA512SUM}" dreach.sha512sum
RUN tar xzf dreach.tar.gz
WORKDIR /tools/dreach/dReal-${DREAL_VERSION}-linux/
RUN ls -l
ENV PATH=$PATH:/tools/dreach/dReal-${DREAL_VERSION}-linux/bin
RUN dReach -h

##################
# Install HyCreate
##################
# version and SHA512 hash (set hash to ' ' to disable hash checking)
# see http://stanleybak.com/projects/hycreate/hycreate.html for available versions
ENV HYCREATE_VERSION 2.81
ENV HYCREATE_FILE_SHA512SUM 'e801d1fb01e112803f83a37d5339c802a638c2cd253d1a5b3794477f69d123ee243206561a51d99502d039f5cc5df859b14dc2c9fd236f58b67b83033d220ca9'

RUN apt-get -qy install unzip
RUN mkdir /tools/hycreate
WORKDIR /tools/hycreate

RUN curl -fL http://stanleybak.com/projects/hycreate/HyCreate2.81.zip > hycreate.zip
# print and check hash
RUN sha512sum hycreate.zip | tee hycreate.sha512sum && grep -q "${HYCREATE_FILE_SHA512SUM}" hycreate.sha512sum
RUN unzip hycreate.zip
WORKDIR /tools/hycreate/HyCreate${HYCREATE_VERSION}/
RUN ls -l
ENV HYPYPATH=$PATH:/tools/hycreate/HyCreate${HYCREATE_VERSION}/
# BUG (reported at https://github.com/verivital/hyst/issues/47 ): hypy expects HyCreate2.8.jar, not HyCreate 2.81.jar.
RUN test -f HyCreate2.8.jar || ln -s HyCreate*.jar HyCreate2.8.jar

##################
# Install Hyst
##################

COPY . /hyst
WORKDIR /hyst/src
RUN ant build
ENV PYTHONPATH=$PYTHONPATH:/hyst/src/hybridpy
ENV HYPYPATH=$HYPYPATH:/hyst/src
ENV PATH=$PATH:/hyst

##################
# As default command: run the tests
##################

CMD ant test

# # USAGE:
# # Build container and name it 'hyst':
# docker build . -t hyst
# # run tests (default command)
# docker run hyst
# # get a shell:
# docker run hyst -it bash
# -> Hyst is available in /hyst/src, tools are in /tools, everything is on the path (try 'hyst -help', 'spaceex --help')
# # run Hyst:
# docker run hyst hyst -help
# # run Hyst via java path:
# docker run hyst java -jar /hyst/src/Hyst.jar -help
# # NOTE: like for a VM, the host system's folders need to be explicitly shared with the guest container.
# # To map /path_on_host to /data in the container:
# docker run -v /path_on_host:/data hyst hyst -t pysim '' -i /data/foo.xml -o /data/bar.xml
