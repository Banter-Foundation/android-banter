#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
# Licensed under the Apache License, Version 2.0

APP_HOME=$( cd "$(dirname "$0")" && pwd -P ) || exit
APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD=$JAVA_HOME/bin/java
else
    JAVACMD=java
fi

exec "$JAVACMD" \
  -Xmx64m -Xms64m \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
