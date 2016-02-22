#! /bin/bash

if [ ! -d "${1}" ] ; then
  echo "usage: ${0} new-rules-dir"
  exit 1
fi

git pull -r

BASE=$(dirname "${0}")
RULES=${BASE}/rulesets

mv ${1}/ruleset*xml "${RULES}/"
rename 's/[() ]/_/g' ${RULES}/ruleset*xml
pushd "${RULES}/"

for f in $(ls -1 ruleset*xml) ; do
  dos2unix "$f"
  ./rename.sh "$f"
done

./buildjson.py > ../_data/rulesets.json

cd ..

git add _data/rulesets.json rulesets/*xml
git diff --staged

echo "commit?"
read a
git commit -m 'add new rules'

echo "push?"
read a
git push

