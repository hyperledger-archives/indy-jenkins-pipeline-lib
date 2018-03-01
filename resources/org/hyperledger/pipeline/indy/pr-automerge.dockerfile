# Development
FROM ubuntu:16.04

ARG uid=1000

# Install environment
RUN apt-get update -y && apt-get install -y \
	git \
	wget \
	python3.5 \
	python3-pip \
	python-setuptools
RUN pip3 install -U \
	pip \
	setuptools \
    requests \
    pygithub
RUN useradd -ms /bin/bash -u $uid sovrin
USER sovrin
WORKDIR /home/sovrin
