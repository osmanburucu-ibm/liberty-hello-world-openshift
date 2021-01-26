#FROM harbor.jkwong.cloudns.cx/ibmcom/websphere-liberty:javaee8-rhel
FROM ibmcom/websphere-liberty:kernel-ubi-min
#FROM ibmcom/websphere-liberty:19.0.0.9-kernel-ubi-min

USER root

#changed 2020.12.07 as microdnf couldn't be found to dnf
#RUN dnf update -y

# Add my app and config
COPY ./target/JavaHelloWorldApp.war /config/apps/
COPY ./src/main/wlp/*.xml /config/

#RUN chown 1001:0 /config/apps/*.war
#RUN chown 1001:0 /config/*.xml


# Optional functionality
# ARG SSL=true
# ARG MP_MONITORING=true

# Add interim fixes (optional)
# COPY --chown=1001:0  interim-fixes /opt/ibm/fixes/


# This script will add the requested XML snippets, grow image to be fit-for-purpose and apply interim fixes
RUN configure.sh
