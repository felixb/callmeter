#! /bin/sh

BASE=$(grep -e string */src/main/res/values/*.xml  | wc -l)
MIN=$(( ${BASE} * 55 / 100 ))
LANGS=$(ls -1d */src/main/res/values-[a-zA-Z\\-]* | grep -ve dpi -e '-v' | xargs -n1 basename)

for lang in ${LANGS} ; do
  i=$(grep -e string */src/main/res/${lang}/*.xml  | wc -l)
  if [ ${i} -lt ${MIN} ] ; then
    echo "remove lang: ${lang} ${i}/${BASE}"
    rm -rf */src/main/res/${lang}
  fi
done
