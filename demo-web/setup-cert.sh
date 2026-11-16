#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p certs ../androidApp/src/debug/res/raw
if [ -e certs/server.key ] || [ -e certs/server.pem ]; then
  if [ ! -f certs/server.key ] || [ ! -f certs/server.pem ]; then
    echo 'Certificado incompleto en demo-web/certs; revise los archivos.' >&2; exit 1
  fi
else
  umask 077
  openssl req -x509 -newkey rsa:2048 -nodes -keyout certs/server.key -out certs/server.pem -days 365 -subj '/CN=localhost' -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1,IP:10.0.2.2' -addext 'basicConstraints=critical,CA:TRUE' -addext 'keyUsage=critical,digitalSignature,keyEncipherment,keyCertSign' >/dev/null 2>&1
fi
cp certs/server.pem ../androidApp/src/debug/res/raw/demo_ca.pem
printf '%s\n' 'Certificado local preparado para HTTPS y para la compilación debug de Android.'
