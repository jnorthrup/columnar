#!/usr/bin/env bash

set -fx
JDIR=$(dirname $0)/../cursor
exec java -classpath "$JDIR/target/*:$JDIR/target/lib/*" ${EXECMAIN:=columnar.HelpMainKt} "$@"
