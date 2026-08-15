#!/bin/bash

set -e

echo "Cleaning generated test files..."

rm -rf target
rm -rf test-output
rm -rf collected-logs
rm -rf allure-report
rm -rf swagger-coverage-output
rm -rf build

echo "Cleanup completed."
