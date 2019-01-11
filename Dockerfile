FROM ubuntu:18.04
ENV DEBIAN_FRONTEND=noninteractive
RUN sed 's@archive.ubuntu.com@ftp.fau.de@' -i /etc/apt/sources.list
RUN apt-get update && apt-get -qy install ant python2.7 python-scipy python-matplotlib git libglpk-dev build-essential python-cvxopt python-sympy gimp

##################
# Install Hylaa
##################
# NOTE: we use a fixed hylaa version for reproducible build results
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
# BUG (TODO report) gimp must be installed, otherwise the Hyst tests fail

# version of flowstar,
# sha512sum hash of the .tar.gz download archive of flowstar.
# to disable the hash check, change the line to:
# ENV FLOWSTAR_FILE_SHA512SUM ' '

ENV FLOWSTAR_VERSION 2.0.0
ENV FLOWSTAR_FILE_SHA512SUM '641179b88a2eb965266f3ec0d8adca6726d5b2a172a5686ae59c1b8fc6bb9dc662ef67d95eb8c158175fd1f411e5db7355a83e5dd12fd4d8fb196e27d4988f79'

# ENV FLOWSTAR_VERSION 2.1.0
# ENV FLOWSTAR_FILE_SHA512SUM 'd5243f3bbcdd6bffcaf2f1ae8559278f62567877021981e4443cd90fbf2918e0acb317a2d27724bc81d3a0e38ad7f7d48c59d680be1dd5345e80d2234dd3fe3b'

# BUG (TODO report) Tests fail with flowstar 2.1.0:
#      [exec] Test failed for 0/96 model mcs_8 with flowstar: Error (Tool)
#      [exec] 
#      [exec] Log:
#      [exec] Running Hyst...
#      [exec] Using Hyst to convert model '/hyst/src/tests/integration/models/8d_motor/mcs_8.xml' for flowstar.
#      [exec] Hyst command: java -jar /hyst/src/Hyst.jar -i /hyst/src/tests/integration/models/8d_motor/mcs_8.xml -o /hyst/src/tests/integration/result/mcs_8_flowstar.flowstar -tool flowstar ''
#      [exec] Seconds for Hyst conversion: 3.20214986801
#      [exec] stderr: Error line 31: syntax error

RUN mkdir -p /tools/flowstar
WORKDIR /tools/flowstar
RUN apt-get install -qy curl flex bison libgmp-dev libmpfr-dev libgsl-dev gnuplot 
RUN curl https://www.cs.colorado.edu/~xich8622/src/flowstar-${FLOWSTAR_VERSION}.tar.gz > flowstar.tar.gz
# print hash
RUN sha512sum flowstar.tar.gz 
# check hash
RUN sha512sum flowstar.tar.gz | grep -q "${FLOWSTAR_FILE_SHA512SUM}"
RUN tar xzf flowstar.tar.gz
# TODO check hash of download
WORKDIR /tools/flowstar/flowstar-${FLOWSTAR_VERSION}/
RUN make
ENV PATH=$PATH:/tools/flowstar/flowstar-${FLOWSTAR_VERSION}/


##################
# Install SpaceEx
##################

RUN mkdir -p /tools/spaceex
WORKDIR /tools/spaceex
# We use the SpaceEx 64bit executable file.
# TODO: SpaceEx doesn't provide a publicly available download URL, you need to fill out the registration form first :-( -> Needs to be uploaded somewhere else (which is okay, it's open source).
# RUN curl http://spaceex.imag.fr/sites/default/files/downloads/private/spaceex_exe-0.9.8f.tar.gz?h=l4cbff53sbjufvn3joktspoea5 | tar xz
COPY ./spaceex_exe-0.9.8f.tar.gz .
RUN tar xzf ./spaceex_exe-0.9.8f.tar.gz
RUN apt-get install -qy plotutils
RUN ./spaceex_exe/spaceex --version
ENV PATH=$PATH:/tools/spaceex/spaceex_exe/

##################
# Install Hyst
##################

COPY . /hyst
WORKDIR /hyst/src
# Bug in sympy < 1.2: "TypeError: argument is not an mpz" (probably https://github.com/sympy/sympy/issues/7457, was fixed Nov 2017)
# -> we use sympy 1.2
RUN apt-get -qy install python-pip python-sympy- && pip install sympy==1.2
RUN ant build

##################
# Run tests
##################

# BUG (TODO report)  Hyst integration tests fail if not Hylaa, SpaceEx and Flowstar are installed.

ENV PYTHONPATH=$PYTHONPATH:/hyst/src/hybridpy
ENV HYPYPATH=/hyst/src
RUN ant test
