#!/bin/bash
set -e

echo "🔍 Variabili nel container:"
env | grep -E "JBOSS|WILDFLY"

echo "[WildFly] Avvio con JBOSS_MODE: ${JBOSS_MODE}"

sh /opt/jboss/wildfly/bin/add-user.sh -m -u "${WILDFLY_ADMIN_USER}" -p "${WILDFLY_ADMIN_PASSWORD}" --silent

if [ "${JBOSS_MODE}" = "domain" ]; then
  echo "→ Domain mode"
  exec /opt/jboss/wildfly/bin/domain.sh -b 0.0.0.0 -bmanagement 0.0.0.0
else
  echo "→ Standalone mode"
  exec /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0
fi