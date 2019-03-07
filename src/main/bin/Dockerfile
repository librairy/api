FROM librairy/modeler-topics-service:latest

ADD model /bin
ENV SWAGGER_TITLE="Wiki Model"
ENV SWAGGER_DESCRIPTION="Topic Model created from (EN) Wikipedia Dump 20180420"
ENV SWAGGER_CONTACT_NAME="librAIry"
ENV SWAGGER_CONTACT_EMAIL="cbadenes@fi.upm.es"
ENV SWAGGER_CONTACT_URL="http://librairy.linkeddata.es"
ENV SWAGGER_LICENSE_NAME="Apache License Version 2.0"
ENV SWAGGER_LICENSE_URL="https://www.apache.org/licenses/LICENSE-2.0"
ENTRYPOINT exec java $JAVA_OPTS -server -jar /app.jar
