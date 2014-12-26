# List of versions and changes for Call Meter 3G aka cm3

## 3.11.5
 * fix units are shown as seconds for mixed plans (issue #45)

## 3.11.4
 * fix internationalizing number groups with prefixes (issue #41)
 * add date format DD MMM. YYYY (issue #38)

## 3.11.3
 * fix behavior of "Is WebSMS" (issue #31)
 * fix Number match issue (issue #32)
 * fix Call Meter 3G not recognizing SMS sent with WebSMS (issue #33)
 * set timezone when importing default rules (issue #6)
 * migrate from google code to github.com (issue #1)
 
## 3.11.2
 * add support for some more dual sim devices (google code issue #163)
 * fix default rule set for second sim if any (google code issue #949)
 * fix closing dialog after importing (google code issue #959)
 * fix for second bill period (google code issue #956)

## 3.11.1
 * fix alert strings for Hungarian
 * fix some minor FCs

## 3.11
 * fix for Sony's broken android implementation
 * fix missing alerts on 80% and 100% plan usage (google code issue #490)
 * add sim id to csv export (google code issue #945)

## 3.10
 * fix RuleEdit for dual sim phones (google code issue #898)
 * filter sms by sim id (google code issue #163)
 * new default rule set with dual sim/websms/voip if available (google code issue #163)
 * remove translation with <55% translated texts

## 3.9.1
 * handle merged plans in LogsFragment (google code issue #877)
 * add Serbian translation (thanks to Igor Kutlarevic)
 * add Portuguese translation (thanks to Sérgio Marques)
 * update translations

## 3.9
 * show last day of bill period (google code issue #856)
 * fix multi sim on ZTE V970 (google code issue #163)
 * confirm deleting plans/rules (google code issue #853)
 * tune widget layout

## 3.8.4
 * fix % in number groups (google code issue #833)
 * set widget color directly (google code issue #834)
 * minor fixes
 * update translations

## 3.8.3
 * show confirmation when deleting groups (google code issue #827)
 * sort plans/rules by buttons instead of menu entries (google code issue #828)
 * add russian translation (massive thanks to Dmitriy Rublev)
 * update translations

## 3.8.2
 * hide total stats by default (google code issue #812)
 * hide amount for "total merger plans" (google code issue #633)
 * include/exclude numbers for MMS (google code issue #775)
 * fix showing total cost in bill period (google code issue #819)

## 3.8.1
 * fix limit calculation for cost limits (google code issue #553)
 * fix mixed plans merging data plans (google code issue #802)
 * fix cost in mixed plans merging other plans (google code issue #801)

## 3.8
 * show users' rules in app
 * fix custom bill mode settings
 * update translation

## 3.7.2
 * fix import of rules with "longdescription" (google code issue #790)

## 3.7.1
 * fix cost in merged plans (reset stats please) (google code issue #785)
 * fix progress bar size for android 3+ (google code issue #774)
 * some more fixes (google code issue #773)

## 3.7
 * fix limit breaking calls/data (google code issue #301)
 * fix cost settings help for plans without limit (google code issue #365)
 * merge data in mixed plans (google code issue #619)
 * hide progressbar for unlimited bill periods (google code issue #707)
 * strip anything but first x seconds (google code issue #629, issue #712)
 * deleting last call will stay deleted until reset stats (google code issue #764)
 * fix & in exports (google code issue #770)
 * fix numbers/hours imports (google code issue #771)
 * fix hidden help/summary
 * fix float cost limits
 * minor fixes

## 3.6.4
 * fix FC on some samsung phones (google code issue #767)
 * fix out of memory exception
 * minor fixes (google code issue #765)

## 3.6.3
 * basic dual sim support (google code issue #163)
 * export/impor XML file
 * fix moving plans/rules (google code issue #746)
 * add widget for "last log" (google code issue #755)
 * fix some FCs
 * minor fixes

## 3.6.2
 * fix FC when exporting rules
 * fix some more settings (google code issue #735)

## 3.6.1
 * fix cost settings (google code issue #733)

## 3.6
 * export logs as csv file (google code issue #719)
 * reimplement preferences
 * fix minor bugs
 * update translations

## 3.5.1
 * fix some FCs
 * fix dark theme
 * fix titles in main view (google code issue #711)

## 3.5
 * update ActionBarScherlock 4.1
 * prepare for android 4.1 aka. jelly bean
 * fix some FCs (google code issue #703 and more)
 * update translations

## 3.4.2
 * fix 31d bill period (google code issue #665)
 * update translations

## 3.4.1
 * show past bill periods as 100% (google code issue #662)
 * fix import/export of rules with new and old bill periods

## 3.4
 * add bill period length 14d 15d 31d 1m+1d (google code issue #650)
 * fix merged cost limits (google code issue #579)
 * minor UI fixes (google code issue #626, issue #658)
 * remove stale widgets (google code issue #630)
 * text in number groups (google code issue #625)
 * fix prepaid cost in widget (google code issue #640)
 * show up to 20plans in "ask for plans" activity (google code issue #643)

## 3.3.1
 * fix FC on android 1.6

## 3.3
 * fix FC on android 1.6
 * fix sorting plans/rules (google code issue #606)
 * fix sum of cost for multi bill period plans (google code issue #594)
 * fix accuracy of data stats (esp. for samsung devices) (google code issue #603)
 * fix widget for galaxy nexus (google code issue #610)
 * configure existing widgets
 * add widget for bill period (google code issue #332)
 * show intro for new users
 * data stats for acer e130 (google code issue #615)

## 3.2.2
 * fix FC
 * update translations

## 3.2.1
 * fix FC (google code issue #602)
 * fix unlimited bill periods
 * add data log record (google code issue #604)
 * fix color of in widget (google code issue #605)
 * update translations

## 3.2
 * improve performance (google code issue #592)
 * fix setting text size
 * fix donation view (google code issue #593)
 * fix date and cost format in logs view (google code issue #595)

## 3.1
 * RECALCULATON OF STATS NEEDED! this will take some time
 * disable rules (google code issue #584)
 * more filters in logs view (google code issue #548, issue #549)
 * major change in design (google code issue #414)
 * major change in database queries (google code issue #579, issue #587)
 * update translations

## 3.0.1
 * minor fix release

## 3.0
 * generic traffic for FroYo+ devices (google code issue #464, issue #527, issue #547)
 * import rule sets from sd card (google code issue #372)
 * update translations

## 3.0 rc15
 * fix data stats (google code issue #509)
 * fix some minor FCs

## 3.0 rc14
 * simple preferences for default rule set
 * reimport default rule set, exlucde numbers for default rule set
 * add support for a601 and htc thunderbolt
 * update translations
 * minor fixes

## 3.0 rc13
 * Fix Samsung Phones (google code issue #480)
 * Translate Default Rule Set

## 3.0 rc12
 * Fix Samsung Galaxy S 2

## 3.0 rc11
 * show icon / small widget (thanks to luca niccoli for both)
 * update translations

## 3.0 rc10
 * set text size/color in widget (google code issue #314)
 * set background color in widget (google code issue #398)
 * add support for LG-P990 (google code issue #432)
 * rename data stats by default (google code issue #441)
 * update translation

## 3.0 rc9
 * Add support for GT-S5830 and possible all future samsung devices (google code issue #426)
 * major inner changes (google code issue #400)
 * update translations

## 3.0 rc8
 * fix an other sunday bug (google code issue #373)
 * allow to split messages at fixed 160 chars  (google code issue #370)
 * fix messages to yourself (google code issue #386)
 * add support for GT-I5510 (google code issue #390)
 * fix custom currency symbol  (google code issue #402)

## 3.0 rc7
 * fix data logs (google code issue #346)
 * fix sunday (google code issue #373)
 * fix limit+cost for merged+mixed plans  (google code issue #292, issue #293)
 * add remote number for MMS (google code issue #367)
 * update translations
 * add help

## 2.9.6
 * fix info toast after call
 * set custom delimiter / fix delimiter (google code issue #358)
 * add mon-fri for hour groups
 * allow <1 units in mixed plans (google code issue #292)
 * fix limit+cost for mixed+merged plans (google code issue #292, issue #293)
 * export/backup number groups, hour groups, logs (google code issue #184, issue #203)

## 2.9.5
 * set date+time for start of billing period (google code issue #258, issue #345)
 * improve data logging (google code issue #346)
 * match (inter-)national numbers (enter your prefix in preferences) (google code issue #349)
 * show today's stats (google code issue #242)
 * allow multi bill period merger (google code issue #56)
 * show call info after hanging up (google code issue #318)

## 2.9.4
 * hide progress bars (google code issue #328)
 * add bill periods: 4,5,6,12 month
 * strip leading zeros (google code issue #266)
 * hide no cost plans

## 2.9.3
 * Integrate with sipdroid >1.6.1
 * default update interval: 60m
 * fix MMS counter
 * fix mixed+merged limits
 * progressbar color
 * add chinese translation

## 2.9.2
 * Delete old logs (default: 90days)
 * multiselection on groups (google code issue #298)
 * fix NPE

## 2.9.1
 * Fix cost for long sms (google code issue #279)
 * fix cost when reaching limit
 * set name/shortname for widget (google code issue #249)
 * show cost in widget (google code issue #218)
 * show billperiod in widget (google code issue #218)
 * settable sizes (google code issue #245)
 * don't bill first x seconds (google code issue #261)
 * merge plans (google code issue #262)
 * hide empty plans

## 2.8.10
 * Integration with WebSMS
 * set custom locale

## 2.8.9
 * clean groups in rules
 * fix numbergroups
 * fix adding logs
 * fix import from file
 * reduce collected traffic data

## 2.8.8
 * fix for GT-I5500 (google code issue #246)
 * fix cost for data (google code issue #251)
 * decimal limit (google code issue #248)

## 2.8.7
 * Fix length of billing period

## 2.8.6
 * Fix some FCs
 * hide total stats (google code issue #227)
 * prepaid plans (google code issue #86)
 * set textsize 
 * manipulate logs by hand (google code issue #189)
 * fix some more bugs (google code issue #224)
 * add 60/15 bill mode (google code issue #222)

## 2.8.5
 * Fix default displaying values for pans/rules

## 2.8.4
 * Fix some FCs
 * fix number groups with more than one entry
 * readd druch, spanish and portuguese translations

## 2.8.3
 * Fix excluded numbers

## 2.8.2
 * Simplify rules (You must reset them!)
 * fix export
 * fix NPEs
 * set update interval
 * set beginning of record

## 2.8.1
 * Add default rule sets
 * reduce complexity for simple setups
 * some changes to gui
 * force internal install

## 2.7.3
 * Fix uneditable fields
 * fix FC for empty bill modes
 * allow to hide title bar
 * readd polish+russion translation

## 2.7.2
 * History
 * alert
 * widget
 * total expense
 * ...

## 2.7.1
 * Custom date format
 * currency format/symbol
 * speedup boot/shutdown process
 * changing color of limit bars
 * new screens for plans/rules

## 2.7.0
 * Initial alpha of Call Meter 3G
 * Major architecture refactoring
