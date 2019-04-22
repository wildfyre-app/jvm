FROM alpine:3.7

# Get Java
RUN apk update \
 && apk upgrade \
 && apk add --no-cache bash \
 && apk add --no-cache --virtual=build-dependencies unzip \
 && apk add --no-cache curl \
 && apk add --no-cache openjdk8-jre

# Get Python
RUN apk add --no-cache python3 \
 && python3 -m ensurepip \
 && pip3 install --upgrade pip setuptools \
 && rm -r /usr/lib/python*/ensurepip && \
 if [ ! -e /usr/bin/pip ]; then ln -s pip3 /usr/bin/pip ; fi && \
 if [[ ! -e /usr/bin/python ]]; then ln -sf /usr/bin/python3 /usr/bin/python; fi && \
 rm -r /root/.cache

# Install other tools that are needed for tests
RUN apk add --no-cache lsof \
 && apk add --no-cache py3-virtualenv
