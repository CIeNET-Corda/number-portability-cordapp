FROM openjdk:8u171-jre-alpine

ENV NODE_NAME=${NODE_NAME:-Notary} \
    P2PPORT=${P2PPORT:-10002} \
    RPC_ADDRESS=${RPC_ADDRESS:-10003} \
    RPC_ADMINADDRESS=${RPC_ADMINADDRESS:-10043} \
    JDWP_PORT=${JDWP_PORT:-5005} \
    JOLOKIA_PORT=${JOLOKIA_PORT:-7005}

COPY nodes /nodes

EXPOSE $P2PPORT
EXPOSE $RPC_ADDRESS
EXPOSE $RPC_ADMINADDRESS
EXPOSE $JDWP_PORT
EXPOSE $JOLOKIA_PORT

CMD sh -c 'cd /nodes/$NODE_NAME && java -Dname=${NODE_NAME}-corda.jar -Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$JDWP_PORT -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=$JOLOKIA_PORT,logHandlerClass=net.corda.node.JolokiaSlf4Adapter -jar corda.jar'
