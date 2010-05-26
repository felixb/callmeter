#! /bin/sh

v=${1}
vv=$(($(grep -o 'versionCode="[0-9]*"' AndroidManifest.xml | cut -d\" -f2) + 1))
n=$(fgrep app_name res/values/base.xml | cut -d\> -f2 | cut -d\< -f1 | tr -d \ )

sed -i -e "s/android:versionName=[^ >]*/android:versionName=\"${v}\"/" AndroidManifest.xml
sed -i -e "s/android:versionCode=[^ >]*/android:versionCode=\"${vv}\"/" AndroidManifest.xml
sed -i -e "s/app_version\">[^<]*/app_version\">${v}/" res/values/base.xml

git diff

ant debug || exit

mv bin/*-debug.apk ~/public_html/h/flx/ 2> /dev/null

echo "enter for commit+tag"
read a
git commit -am "bump to v${v}"
git tag -a "v${v}" -m "${n}-${v}"

