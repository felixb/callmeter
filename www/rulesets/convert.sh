#! /bin/bash

if [ ! -f $1 ] ; then
  echo "file $1 not found!" >&2
  exit 1
fi

alias urlencode='python -c "import sys, urllib as ul; print ul.quote_plus(sys.argv[1])"'
alias urldecode='python -c "import sys, urllib as ul; print ul.unquote_plus(sys.argv[1])"'

version=$(head -n1 $1)

echo '<?xml version="1.0" encoding="utf-8"?>'
echo '<ruleset version="2">'

title=$(head -n2 $1 | tail -n1)
title_decode=$(python -c "import sys, urllib as ul; print ul.unquote_plus('"${title}"')")
bn=$(basename $1)
cc=$(echo $bn | head -c2)
echo "  <title>$(echo "${title_decode}" | head -n1 | sed -e 's:<:&lt;:g' -e 's:>:&gt;:g' -e 's:&:&amp;:g')</title>"
if [ $(echo "${title_decode}" | wc -l) -gt 1 ] ; then
  echo "  <description>"
  echo "$(echo "${title_decode}" | tail -n+2 | sed -e 's:<:&lt;:g' -e 's:>:&gt;:g' -e 's:&:&amp;:g')"
  echo "  </description>"
fi
if (echo $cc | grep -qi '[a-z]') ; then
  if ( grep -q "^${cc}" countries.csv ) ; then
    cc=$(grep "^${cc}" countries.csv |cut -d, -f2)
  fi
  echo "  <country>$(echo "${cc}" | sed -e 's:<:&lt;:g' -e 's:>:&gt;:g' -e 's:&:&amp;:g')</country>"
  echo "  <provider>$(python -c "import sys, urllib as ul; print ul.quote_plus(sys.argv[1])" "$(echo $bn | cut -d_ -f3 | sed -e 's:.export::')" | sed -e 's:<:&lt;:g' -e 's:>:&gt;:g' -e 's:&:&amp;:g')</provider>"
else
  echo "  <country>common</country>"
fi

if [ -f "${1}.link" ] ; then
  echo "  <link>$(<${1}.link)</link>"
fi
if [ -f "${1}.descr" ] ; then
  echo "  <longdescription>"
  sed -e 's:<:&lt;:g' -e 's:>:&gt;:g' -e 's:&:&amp;:g' "${1}.descr"
  echo "  </longdescription>"
fi


for t in plans rules numbersgroup hoursgroup ; do # numbers hours
  if [ "$t" == "hoursgroup" -o "$t" == "numbersgroup" ] ; then
    to="${t}s"
    te="${t}"
  else
    to="${t}"
    te="${t//s}"
  fi
  if (grep -q "^$t " $1) ; then
    echo "  <$to>"
    grep "^$t " $1 | while read line ; do
      echo "    <$te>"
      if [ "${t}" == "plans" -o "${t}" == "rules" ] ; then
        if ( echo "${line}" | grep -vq '_order:' ) ; then
          if ( echo "${line}" | grep -q '_plan_type:2:' ) ; then
            id=0
          else
            id=$(echo "${line}" | grep -o ' _id:[^:]*' | cut -d: -f2)
          fi
          line=$(echo "${line}" | sed -e 's/$/_order:'${id}':#:/')
        fi
      fi
      echo "$line" | cut -d\  -f2- | sed -e "s/:#:/\n/g" | grep ':..*$' | while read attr ; do
        an="${attr//:*}"
        av="${attr##${an}:}"
        if [ "$version" == "0" -a "$te" == "plan" -a "$an" == "_billperiod" ] ; then
          if [ $av -ge 6 ] ; then
            av=$(( $av + 4 ))
          elif [ $av -ge 3 ] ; then
            av=$(( $av + 3 ))
          elif [ $av -ge 2 ] ; then
            av=$(( $av + 2 ))
          fi
        fi
        av=$(echo $av | sed -e 's:<:&lt;:g' -e 's:>:&gt;:g' -e 's:&:&amp;:g')
        echo "      <$an>$av</$an>"
      done
      if [ "${te}" == "numbersgroup" -o "${te}" == "hoursgroup" ] ; then
        gid=$(echo "$line" | cut -d\  -f2- | grep -o '_id:[^:]*' | cut -d: -f2)
        if [ "${te}" == "numbersgroup" -a -n "${gid}" ] ; then
          if (grep '^numbers ' $1 | grep -q "_gid:${gid}:") ; then
            echo "      <numbers>"
            grep '^numbers ' $1 | grep "_gid:${gid}:" | while read item ; do
              echo "        <number>"
               echo "$item" | cut -d\  -f2- | sed -e "s/:#:/\n/g" | grep ':..*$' | while read attr ; do
                an="${attr//:*}"
                av="${attr##${an}:}"
                echo "          <$an>$av</$an>"
              done
              echo "        </number>"
            done
            echo "      </numbers>"
          fi
        elif [ "${te}" == "hoursgroup" -a -n "${gid}" ] ; then
          if (grep '^numbers ' $1 | grep -q "_gid:${gid}:") ; then
            echo "      <hours>"
            grep '^hours ' $1 | grep "_gid:${gid}:" | while read item ; do
              echo "        <hour>"
               echo "$item" | cut -d\  -f2- | sed -e "s/:#:/\n/g" | grep ':..*$' | while read attr ; do
                an="${attr//:*}"
                av="${attr##${an}:}"
                echo "          <$an>$av</$an>"
              done
              echo "        </hour>"
            done
            echo "      </hours>"
          fi
        fi
      fi
      echo "    </$te>"
    done
    echo "  </$to>"
  fi
done

echo '</ruleset>'
