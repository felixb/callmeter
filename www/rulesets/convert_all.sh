#! /bin/bash

if [ -z "$1" ] ; then
  rm *.xml
  [ -e rename.lst ] && rm rename.lst
fi

for f in ../*export ../*xml ; do
  if [ -n "$1" ] ; then
    f=$1
  fi
  echo "convert: $f"
  bf=$(basename $f)
  nf=$(echo ${bf} | sed -e 's:.export:.xml:')
  if [ "${bf}" != "${nf}" ] ; then
    ./convert.sh "${f}" > "${nf}"
  else
    cp "${f}" "${nf}"
  fi
  ./rename.sh "${nf}" | tee -a rename.lst
  if [ -n "$1" ] ; then
    break
  fi
done
