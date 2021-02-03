#!/bin/bash

dropdb --if-exists nsl-test &&
createdb nsl-test &&
psql -f ddl.sql nsl-test &&
psql -f test-data.sql nsl-test