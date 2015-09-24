#! /bin/sh

crowdin-cli download
sed -e 's/ formatted="false"//' -e 's/string name=/string formatted="false" name=/' -i CallMeter3G/src/main/res/values-*/*
