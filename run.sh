#!/bin/bash

args=("$@")
for i in `seq 1 ${args[0]}`; do 
  ant send & 
done
