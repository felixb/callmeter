#!/bin/bash

cp index.html /tmp/index.html
sed -e '/<h3>/,$ d' /tmp/index.html > index.html

echo "<h3>Rule sets</h3>" >> index.html
echo "<ul>" >> index.html
for f in *.export ; do
	rsname="$(echo ${f} | cut -d_ -f2- | cut -d. -f1 | tr '_' ' ')"
	echo "<li><a href=\"#f_${f}\">${rsname}</a></li>" >> index.html
done
echo "</ul>" >> index.html

for f in *.export ; do
	rsname="$(echo ${f} | cut -d_ -f2- | cut -d. -f1 | tr '_' ' ')"
	echo "<div id=\"f_${f}\">" >> index.html
	echo "<h3>${rsname}</h3>" >> index.html
	php -r "echo nl2br(urldecode('$(head -n2 ${f} | tail -n1)'));" >> index.html
	echo "<br />" >> index.html
	echo "<a href=\"import://callmeter.android.ub0r.de/www.ub0r.de/android/callmeter/rulesets/${f}\">direct</a>" >> index.html
	echo "<a href=\"http://chart.apis.google.com/chart?chs=200x200&amp;cht=qr&amp;chl=import%3A%2F%2Fcallmeter.android.ub0r.de%2Fwww.ub0r.de%2Fandroid%2Fcallmeter%2Frulesets%2F${f}\">barcode</a>" >> index.html
	if [ -e "${f}.descr" ] ; then
		echo "<a href=\"http://www.ub0r.de/android/callmeter/rulesets/${f}.descr\">description</a>" >> index.html
	fi
	echo "</div>" >> index.html
done

echo "" >> index.html

sed -ne '/<\/font>/,$ p' /tmp/index.html >> index.html

rm /tmp/index.html
