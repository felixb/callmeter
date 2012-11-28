#! /bin/bash

if [ -z "$1" ] ; then
  exit 1
fi

f=$1
country=$(grep '<country>' $f | cut -d\> -f2 | cut -d\< -f1)
provider=$(grep '<provider>' $f | cut -d\> -f2 | cut -d\< -f1)
title=$(grep '<title>' $f | cut -d\> -f2 | cut -d\< -f1 | sed -e "s#$(echo ${provider//#} | tr '+*()[]{}^$' '..........') *##")
if [ "${provider}" == "o2o" ] ; then
  provider=o2
elif [ "${provider}" == "vodafone" ] ; then
  provider=Vodafone
elif [ "${provider}" == "t-mobile" ] ; then
  provider=T-Mobile
elif [ "${provider}" == "mtel" ] ; then
  provider=Mtel
fi

if [ "$country" == "common" ] ; then
  nf="00_$(echo ${title} | tr ' ' '_' | tr -Cd 'a-zA-Z0-9_\-.').xml"
else
  nf="$( echo "${country}_${provider}_${title}" | tr ' ' '_' | tr -Cd 'a-zA-Z0-9_\-.' | head -128c).xml"
  sed -e "s#<provider>.*#<provider>${provider}</provider>#" -i "${f}"
  sed -e "s#<title>.*#<title>${title}</title>#" -i "${f}"
fi

echo "$nf    <-      $f"
mv "${f}" "${nf}"

