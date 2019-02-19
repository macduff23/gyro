#!/bin/bash

mkdir -p dist
rm -rf dist/beam-rt

cd dist

jlink --no-header-files \
    --no-man-pages \
    --add-modules java.logging,java.management,java.naming,java.scripting,java.xml,jdk.unsupported,jdk.xml.dom,java.desktop,java.instrument,java.compiler \
    --output beam-rt

zip -r beam-0.5.zip beam-rt beam