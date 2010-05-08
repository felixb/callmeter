#! /bin/sh

for f in $(find src/ -name \*java) ; do
	sed -e 's:///*Log.v:Log.v:' -i $f
	sed -e 's:///*Log.d:Log.d:' -i $f
done

for f in $(find . -name AndroidManifest.xml) ; do
	sed -e 's/android:debuggable="false"/android:debuggable="true"/' -i $f
done
