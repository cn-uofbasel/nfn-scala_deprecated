FROM haroon/docker-oracle-jdk7
MAINTAINER Basil Kohler<basil.kohler@gmail.com>

ENV CCNL_HOME /ccn-lite
ENV CCNL_PORT 9999

RUN apt-get update && apt-get install -y libssl-dev build-essential git wget

ADD . /var/ccn-lite
WORKDIR /var/ccn-lite/src
RUN make clean all

CMD ./ccn-lite-relay -s ccnx2014 -v 99 -u $CCNL_PORT -d ../test/ccntlv -x /tmp/ccn-lite-relay.sock
