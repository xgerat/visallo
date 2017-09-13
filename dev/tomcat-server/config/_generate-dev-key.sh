#!/bin/sh

keytool \
  -genkey \
  -keyalg RSA \
  -ext san=dns:localhost,ip:127.0.0.1,ip:::1 \
  -alias visallo-vm.visallo.org \
  -keystore visallo-vm.visallo.org.jks \
  -storepass password \
  -validity 360 \
  -keysize 2048
