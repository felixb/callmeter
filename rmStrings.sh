#! /bin/bash

for p in $@ ; do
	echo "remove string: $p"
	for f in $(ls res/values-*/strings.xml) ; do
		grep -Fv "name=\"${p}\"" $f > /tmp/callmeter.rmStrings
		mv /tmp/callmeter.rmStrings $f
	done
done
