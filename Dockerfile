FROM openjdk:8u171-alpine3.7
RUN addgroup -g 5000 nsl_user; adduser -u 5000 -D nsl_user -G nsl_user
COPY build/libs/mapper-mn-*-all.jar mapper-mn.jar
EXPOSE 8080
USER nsl_user
#CMD /bin/sh
CMD java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dcom.sun.management.jmxremote -noverify ${JAVA_OPTS} \
-Dmicronaut.config.files=/etc/nsl/nsl-mapper-config-mn.groovy \
-jar mapper-mn.jar
