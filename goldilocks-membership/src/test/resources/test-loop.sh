#!/bin/bash
error_found=2
counter=0
mvn clean test
let counter=counter+1
while [ $error_found -eq 2 ]; do
	mvn test
    error_found=`grep -ir error ./target/logs/* ./target/surefire-reports/* | wc -l`
	let counter=counter+1
	echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
	echo ">> Errors found: $(($error_found - 2))" 
	echo ">> Round: $counter"
	echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
done
