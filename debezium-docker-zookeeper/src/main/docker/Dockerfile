FROM debezium-jre8

MAINTAINER rhauch

# Debian: Install curl and wget ...
RUN apt-get update && \
    apt-get install -y openssh-server &&\
    rm -rf /var/lib/apt/lists/*

#
# Download and install Zookeeper
#
ENV ZK_VERSION=3.4.6 \
    ZK_HOME=/zookeeper \
    ZK_USER=zookeeper
RUN mkdir $ZK_HOME && \
    mkdir $ZK_HOME/data && \
    mkdir $ZK_HOME/txns && \
    mkdir $ZK_HOME/logs && \
    wget -q -O - $(curl --stderr /dev/null https://www.apache.org/dyn/closer.cgi\?as_json\=1 | sed -rn 's/.*"preferred":.*"(.*)"/\1/p')zookeeper/zookeeper-$ZK_VERSION/zookeeper-$ZK_VERSION.tar.gz | tar -xzf - -C $ZK_HOME --strip-components 1

# Set the working directory to the Zookeeper home directory
WORKDIR $ZK_HOME

#
# Customize the Zookeeper Log4J configuration files
#
COPY ./zoo.cfg $ZK_HOME/conf/zoo.cfg
RUN sed -i -r -e "s|(\\$\\{zookeeper.log.dir\\})|$ZK_HOME/logs|g" \
              -e "s|(\\$\\{zookeeper.tracelog.dir\\})|$ZK_HOME/logs|g" \
              -e "s|(\\$\\{zookeeper.log.file\\})|zookeeper.log|g" \
              -e "s|(\\$\\{zookeeper.tracelog.file\\})|zookeeper_trace.log|g" \
              -e "s|(\[myid\:\%X\{myid\}\]\s?)||g" \
              -e 's|#(log4j.appender.ROLLINGFILE.MaxBackupIndex.*)|\1|g' \
              $ZK_HOME/conf/log4j.properties

# Set up a user to run Zookeeper and own all of its files
RUN groupadd $ZK_USER -g 1000 && \
    useradd -d $ZK_HOME -u 1000 -g $ZK_USER -s /bin/false -c "Zookeeper user" $ZK_USER && \
    chown -R $ZK_USER:$ZK_USER $ZK_HOME

# Start the SSH daemon ...    
RUN /etc/init.d/ssh start

# Specify the user which should be used to execute all commands below
USER $ZK_USER

#
# Expose the ports and set up volumes for the data, transaction log, and configuration
#
EXPOSE 2181 2888 3888
VOLUME ["/zookeeper/data","/zookeeper/txns","/zookeeper/conf"]

COPY ./docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["start"]
