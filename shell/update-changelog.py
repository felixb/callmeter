#! /usr/bin/env python

import sys

versions = []

lines = sys.stdin.read().split("\n")
v = None
for l in lines:
  l = l.rstrip()
  if len(l) == 0:
    continue
  if l[0:3] == '## ':
    if v:
      versions.append((v, changes))
    v = l[3:]
    changes = []
  elif l[0:3] == ' * ':
    changes.append(l[3:])

print '<?xml version="1.0" encoding="utf-8"?>'
print '<!-- generated with shell/update-changelog.py -->'
print '<resources>'
print '  <string-array name="updates" translatable="false">'
for (v, changes) in versions:
  line = ', '.join(changes)
  line = line.replace("'", "\\'")
  line = line.replace("%", "%%")
  line = line.replace("&", "&amp;")
  line = line.replace("<", "&lt;")
  line = line.replace(">", "&gt;")
  line = line.replace("...", "&#8230;")
  print '    <item>v' + v + ': ' + line + '</item>'
print '  </string-array>'
print '</resources>'
