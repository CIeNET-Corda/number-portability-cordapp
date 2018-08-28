FROM 8u171-jre-alpine

COPY build/nodes /nodes

EXPOSE 10003
EXPOSE 10043
WORKDIR /nodes/Notry
RUN java -Dname=Notary-corda.jar -Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7005,logHandlerClass=net.corda.node.JolokiaSlf4Adapter -jar corda.jar &

EXPOSE 10006
EXPOSE 10046
WORKDIR /nodes/CTCC
RUN java -Dname=CTCC-corda.jar -Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7006,logHandlerClass=net.corda.node.JolokiaSlf4Adapter -jar corda.jar &

EXPOSE 10009
EXPOSE 10049
WORKDIR /nodes/CMCC
RUN java -Dname=CMCC-corda.jar -Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5007 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7007,logHandlerClass=net.corda.node.JolokiaSlf4Adapter -jar corda.jar &

EXPOSE 10012
EXPOSE 10052
WORKDIR /nodes/CUCC
RUN java -Dname=CUCC-corda.jar -Dcapsule.jvm.args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5008 -javaagent:drivers/jolokia-jvm-1.3.7-agent.jar=port=7008,logHandlerClass=net.corda.node.JolokiaSlf4Adapter -jar corda.jar &
