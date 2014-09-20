#! /bin/sh

targetdir=$(find . -name translation.stats | head -n1 | xargs dirname)
if [ -n "${targetdir}" ] ; then
  cd ${targetdir}
fi

BASE=$(grep -F 'res/values:' translation.stats | cut -d: -f2)
MIN=$(( ${BASE} * 55 / 100 ))

for lang in $(grep 'res/values-[a-zA-Z\\-]*:' translation.stats | cut -d: -f1) ; do
  i=$(grep -F "${lang}:" translation.stats | cut -d: -f2)
  if [ ${i} -lt ${MIN} ] ; then
    echo "remove lang: ${lang} ${i}/${BASE}"
    rm -rf "${lang}"
  fi
done
