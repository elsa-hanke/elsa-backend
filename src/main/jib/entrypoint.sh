#!/bin/sh

echo "The application will start in ${JHIPSTER_SLEEP}s..." && sleep ${JHIPSTER_SLEEP}
exec java ${JAVA_OPTS} --add-opens java.base/sun.nio.ch=ALL-UNNAMED -noverify -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom -cp /app/resources/:/app/classes/:/app/libs/* "fi.elsapalvelu.elsa.ElsaBackendApp"  "$@"
