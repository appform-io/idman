#!/bin/bash

IDMAN_SERVER_CONFIG_FILE=/config/docker.yml

if [ -z "${CONFIG_PATH}" ]; then
    echo "No CONFIG_PATH defined. We shall be using default config from ${IDMAN_SERVER_CONFIG_FILE}"
    if [ -z "${SHARDS}" ]; then
        :
    else
        echo "You cannot override shards unless you change the config file. Volume mount a new config and set the path in CONFIG_PATH env variable"
        exit 1
    fi
else
    if [ -f ${IDMAN_SERVER_CONFIG_FILE} ]; then
        IDMAN_SERVER_CONFIG_FILE=${CONFIG_PATH}
    else
        echo "Defined CONFIG_PATH (${CONFIG_PATH}) doesn ot look like a proper file. Using default: ${IDMAN_SERVER_CONFIG_FILE}"
    fi
fi

EXEC_CMD="java -Dfile.encoding=utf-8 -XX:+${GC_ALGO-UseG1GC} -Xms${JAVA_PROCESS_MIN_HEAP-1g} -Xmx${JAVA_PROCESS_MAX_HEAP-1g} ${JAVA_OPTS} -jar idman-server.jar server ${IDMAN_SERVER_CONFIG_FILE}"

echo "Number of database shards: ${SHARDS}"

echo "Starting idman-server with command line: ${EXEC_CMD}"
$EXEC_CMD
