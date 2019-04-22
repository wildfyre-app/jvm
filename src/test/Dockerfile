FROM alpine:3.7

# Get the basics
RUN apk update \
 && apk upgrade \
 && apk add --no-cache bash curl wget \
 && apk add --no-cache --virtual=build-dependencies unzip

# Get Java
RUN apk add --no-cache openjdk8-jre openjdk8

# Get Java sources
RUN https://github.com/ensime/openjdk8src/blob/master/src.zip?raw=true \
 && mv src.zip?raw=true /usr/lib/jvm/java-8-openjdk-amd64/src.zip

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
 && apk add --no-cache gcc \
 && apk add --no-cache libffi
