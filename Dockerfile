FROM openjdk:8u171-alpine3.7
COPY build/libs/mapper-mn-*-all.jar mapper-mn.jar
EXPOSE 8080
CMD java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} \
-Dmicronaut.config.files=/etc/nsl/nsl-mapper-config-mn.groovy \
-jar mapper-mn.jar
