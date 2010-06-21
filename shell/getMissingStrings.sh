#! /bin/bash

grep -ho 'name=[^>]*' res/values/strings.xml > /tmp/callmeter.strings.names.orig
grep -ho 'name=[^>]*' ../ub0rlib/res/values/strings.xml | grep -vf /tmp/callmeter.strings.names.orig > /tmp/callmeter.strings.names
cat /tmp/callmeter.strings.names.orig >> /tmp/callmeter.strings.names
rm /tmp/callmeter.strings.names.orig

for f in $(ls res/values-*/strings.xml) ; do
	l=$(echo $(basename $(dirname $f)) | cut -d'-' -f2)
	echo $f >&2
	grep -v '^<!--' $f > /tmp/callmeter.strings.$l
	mv /tmp/callmeter.strings.$l $f

	grep -o 'name=[^>]*' $f > /tmp/callmeter.strings.names.$l

	head -n2 res/values/strings.xml > /tmp/callmeter.strings.$l
	grep 'Copyright (C)' $f >> /tmp/callmeter.strings.$l
	sed -n '4,/\<resources\>/p' res/values/strings.xml >> /tmp/callmeter.strings.$l
	for s in $(cat /tmp/callmeter.strings.names) ; do
		if ( grep "$s" $f | grep -vq '<!-- ' ) ; then
			if (grep -h "$s" ../ub0rlib/res/values/strings.xml res/values/strings.xml | grep -q 'string-array') ; then
				pos=$(grep -n "$s" $f | cut -d: -f1)
				if [ -n "$pos" ] ; then
					sed -n $f -e "$pos,/<\/string-array>/p" >> /tmp/callmeter.strings.$l
				fi
			else
				grep "$s" $f | head -n1 >> /tmp/callmeter.strings.$l
			fi
		else
			if (grep -h "$s" ../ub0rlib/res/values/strings.xml res/values/strings.xml | grep -q 'string-array') ; then
				sed -n ../ub0rlib/res/values/strings.xml res/values/strings.xml -e "/$s/,/<\/string-array>/p" | \
					sed -e 's:^:<!-- :' -e 's:$: -->:' >> /tmp/callmeter.strings.$l
			else
				grep -h "$s" ../ub0rlib/res/values/strings.xml res/values/strings.xml | tail -n1 | \
					sed -e 's:^:<!-- :' -e 's:$: -->:' >> /tmp/callmeter.strings.$l
			fi
		fi
	done
	echo '</resources>' >> /tmp/callmeter.strings.$l
	mv /tmp/callmeter.strings.$l $f
	
	#for s in $(grep -vf /tmp/callmeter.strings.names.$l /tmp/callmeter.strings.names) ; do
	#	echo $s >&2
	#	pos=$(grep -n "$s" res/values/strings.xml | cut -d: -f1)
	#	if $(grep -n "$s" res/values/strings.xml | grep -v 'string-array' > /dev/null) ; then
	#		line=$(grep "$s" res/values/strings.xml | sed -e 's:^:<!-- :' -e 's:$: -->:')
	#		sed -e "${pos}i${line//\\/\\\\}" -i $f
	#	else
	#		line=$(sed -n res/values/strings.xml -e "$pos,/<\/string-array>/p" | sed -e 's:^:<!-- :' -e 's:$: -->:')
	#		line=$(echo $line | sed -e 's: <!--:\\n<!--:g')
	#		sed -e "${pos}i${line}" -i $f
	#	fi
	#done

	sed 	-e 's:[ 	]*<string:	<string:' \
		-e 's:[ 	]*</string-:	</string-:' \
		-e 's:[ 	]*<item>:		<item>:' \
		-e 's: *$::' \
		-i $f
done
