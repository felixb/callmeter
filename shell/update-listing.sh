#!/bin/bash

LANGS="
:en-US
ca:ca
cs:cs-CZ
de:de-DE
el:el-GR
el:es-ES
fr:fr-FR
hr:hr
hu:hu-HU
it:it-IT
iw:iw-IL
nl:nl-NL
pl:pl-PL
pt:pt-PT
ru:ru-RU
sk:sk
sr:sr
tr:tr-TR
zh-rTW:zh-TW
"

BASEDIR="$(dirname $0)/../CallMeter3G/src/main"
FALLBACKDIR="${BASEDIR}/res/values"

function grep_xml() {
  sed \
    -e 's/.*<string[^>]*>//' \
    -e 's/<\/string>.*//' \
    -e "s/\\\'/'/g" \
    -e 's/\\n/\n/g'
}

# $1 = field name
# $2 = srcdir
# $3 = file name
function grep_text() {
  (grep -F "name=\"$1\"" "$2/$3" || grep -F "name=\"$1\"" "${FALLBACKDIR}/$3") | grep_xml
}

for lang in ${LANGS} ; do
  srclang=${lang//:*}
  trglang=${lang//*:}

  if [ -n "${srclang}" ] ; then
    srclang="-${srclang}"
  fi

  srcdir="${BASEDIR}/res/values${srclang}"
  trgdir="${BASEDIR}/play/${trglang}/listing"

  echo "${srcdir} => ${trgdir}"

  grep_text market_app_name ${srcdir} market.xml    > "${trgdir}/title"
  grep_text market_promo ${srcdir} market.xml       > "${trgdir}/shortdescription"
  grep_text market_about ${srcdir} market.xml       > "${trgdir}/fulldescription"
  echo ""                                           >> "${trgdir}/fulldescription"
  grep_text market_oss ${srcdir} market.xml         >> "${trgdir}/fulldescription"
  echo ""                                           >> "${trgdir}/fulldescription"
  grep_text market_cm2 ${srcdir} market.xml         >> "${trgdir}/fulldescription"
  echo ""                                           >> "${trgdir}/fulldescription"
  grep_text market_permissions ${srcdir} market.xml >> "${trgdir}/fulldescription"
  echo ""                                           >> "${trgdir}/fulldescription"
  grep_text market_translation ${srcdir} market.xml >> "${trgdir}/fulldescription"
  echo ""                                           >> "${trgdir}/fulldescription"
  grep_text market_website ${srcdir} market.xml     >> "${trgdir}/fulldescription"
done