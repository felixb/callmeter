#! /usr/bin/env python

#
# read exported logs in csv format from standard in and generates xml on standard out
#
import fileinput
import string
import time

print '<?xml version="1.0" encoding="utf-8"?>'
print '<logs version="2">'
print '  <title>generated log file</title>'
print '  <logs>'

for line in fileinput.input():
  if not line.startswith('20'):
    continue
  data=line.strip().split(';')
  date=time.strptime(data[0], '%Y%m%d%H%M%S')
  if data[1] == 'Data':
    ltype='7'
  elif data[1] == 'Call':
    ltype='4'
  elif data[1] == 'SMS':
    ltype='5'
  elif data[1] == 'MMS':
    ltype='6'
  else:
    continue
  if data[2] in ['in']:
    direction='0'
  elif data[2] in ['out', 'uit']:
    direction='1'
  else:
    continue
  amount=data[5].replace(',','.')
  namount=amount.strip(string.letters)
  if amount.endswith('kB'):
    amount=str(int(float(namount) * 1024))
  if amount.endswith('MB'):
    amount=str(int(float(namount) * 1024 * 1024))
  if amount.endswith('GB'):
    amount=str(int(float(namount) * 1024 * 1024 * 1024))
  if amount.endswith('TB'):
    amount=str(int(float(namount) * 1024 * 1024 * 1024 * 1024))
  elif amount.endswith('B'):
    amount=str(int(float(namount)))
  print '    <log>'
  print '      <_date>' + str(int(time.mktime(date))) + '</_date>'
  print '      <_type>' + ltype + '</_type>'
  print '      <_direction>' + direction + '</_direction>'
  print '      <_roamed>' + data[3] + '</_roamed>'
  if data[4] != 'null' and data[4] != '':
    print '      <_remote>' + data[4] + '</_remote>'
  print '      <_amount>' + amount + '</_amount>'
  print '    </log>'

print '  </logs>'
print '</logs>'
