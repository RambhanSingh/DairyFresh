#!/usr/bin/env sh
cd "$(dirname "$0")" || exit 1
echo "============================================================"
echo " Milk Distribution"
echo " When you see 'Started MilkApplication', open:"
echo "     http://localhost:8080"
echo " Press Ctrl+C to stop the app."
echo "============================================================"
mvn spring-boot:run
