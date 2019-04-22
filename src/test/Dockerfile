##################################################################
#                                                                #
#       Dockerfile that can be used to execute this              #
#       project's unit tests, with all dependencies validated.   #
#                                                                #
#       It also includes the dependencies for the API,           #
#       however the API itself isn't included. It is the         #
#       user's responsibility to run the API.                    #
#                                                                #
#       This Dockerfile can be found on Dockerhub at             #
#                   clovisai/wildfyre-java                       #
#                                                                #
##################################################################

FROM alpine:3.7

# Get the basics
RUN apk update \
 && apk upgrade \
 && apk add --no-cache bash curl wget \
 && apk add --no-cache --virtual=build-dependencies unzip

# Get Java
RUN apk add --no-cache openjdk8-jre openjdk8

# Get Java sources
ADD https://github.com/ensime/openjdk8src/raw/master/src.zip /usr/lib/jvm/java-8-openjdk-amd64

# Get Python
RUN apk add --no-cache python3 python3-dev py3-virtualenv \
 && python3 -m ensurepip \
 && pip3 install --upgrade pip setuptools \
 && rm -r /usr/lib/python*/ensurepip && \
 if [ ! -e /usr/bin/pip ]; then ln -s pip3 /usr/bin/pip ; fi && \
 if [[ ! -e /usr/bin/python ]]; then ln -sf /usr/bin/python3 /usr/bin/python; fi && \
 rm -r /root/.cache

# Install other tools that are needed for tests
RUN apk add --no-cache lsof \
 && apk add --no-cache git \
 && apk add --no-cache postgresql-dev \
 && apk add --no-cache gcc musl-dev libc-dev \
 && apk add --no-cache libffi libffi-dev

# Temporary
RUN apk add --no-cache zlib zlib-dev

RUN apk add --no-cache libjpeg jpeg jpeg-dev
