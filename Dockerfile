#FROM lscr.io/linuxserver/webtop:ubuntu-mate
FROM ubuntu:noble

RUN apt-get -y update
RUN apt-get -y install default-jre texlive-full dvisvgm
WORKDIR /Protter
