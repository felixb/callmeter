#! /usr/bin/python

import sys
import os
import xml.etree.ElementTree as ET
import json

def parseFile(f):
  print >> sys.stderr, 'parsing:', f
  tree = ET.parse(f)
  root = tree.getroot()

  dict = {}

  list = root.findall('country')
  if len(list) > 0:
    dict['country'] = list[0].text
  else:
    return

  list = root.findall('provider')
  if len(list) > 0:
    dict['provider'] = list[0].text.strip()
  else:
    dict['provider'] = 'common'

  list = root.findall('title')
  if len(list) > 0:
    dict['title'] = list[0].text.strip()
  else:
    return

  list = root.findall('description')
  if len(list) > 0:
    dict['description'] = list[0].text.strip()

  list = root.findall('longdescription')
  if len(list) > 0:
    dict['longdescription'] = list[0].text.strip()

  list = root.findall('link')
  if len(list) > 0:
    dict['link'] = list[0].text

  dict['importurl'] = 'https://felixb.github.io/callmeter/rulesets/' + f
  dict['id'] = 'f_' + f.replace('.xml', '').replace('.', '_')

  return dict

countries = {}

files = [f for f in os.listdir('.') if os.path.isfile(f) and f.endswith('.xml')]
files = sorted(files)
for f in files:
  ruleset = parseFile(f)
  country = ruleset['country']
  provider = ruleset['provider']
  if country in countries:
    list = countries[country]
  else:
    list = []
    countries[country] = list
  list.append(ruleset)

for k in countries.keys():
  list = countries[k]
  list = sorted(list, key = lambda a: a['provider'])
  countries[k] = list

print json.dumps(countries, sort_keys=True, indent=2, separators=(',', ': '))
