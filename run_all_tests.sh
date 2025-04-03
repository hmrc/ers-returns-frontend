#!/usr/bin/env bash

sbt clean scalastyleAll coverage test coverageOff coverageReport A11y/test dependencyUpdates
