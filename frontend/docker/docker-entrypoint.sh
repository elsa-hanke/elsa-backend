#!/bin/sh
set -e

# Substitute ONLY ${BACKEND_URL} so that nginx's own variables
# ($uri, $host, $remote_addr, etc.) are left untouched.
envsubst '${BACKEND_URL}' \
  < /etc/nginx/nginx.conf.template \
  > /etc/nginx/conf.d/default.conf

exec nginx -g 'daemon off;'

