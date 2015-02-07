#! /bin/sh

sed -e '1,/^## /d' \
    -e '/^## /,$d' \
    -e '/^\s*$/d' \
    -e 's/^ //' \
    CHANGELOG.md \
    > /tmp/whatsnew

for f in CallMeter3G/src/main/play/*/whatsnew ; do
  cp /tmp/whatsnew ${f}
done

rm /tmp/whatsnew
