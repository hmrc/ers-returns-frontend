#!/usr/bin/env bash

sbt clean  coverage test coverageOff coverageReport A11y/test dependencyUpdates
