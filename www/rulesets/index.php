<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<?

function endsWith($haystack, $needle) {
  $length = strlen($needle);
  $start  = $length * -1; //negative
  return (substr($haystack, $start) === $needle);
}

$isAndroid = preg_match('/Android/', $_SERVER['HTTP_USER_AGENT']);
$location = './';

?>
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Call Meter 3G rule sets</title>
<?
if ($isAndroid) {
  echo '<link rel="stylesheet" type="text/css" href="android.css" />' . "\n";
} else {
  echo '<link rel="stylesheet" type="text/css" href="standard.css" />' . "\n";
}
?>
<script type="text/javascript">
  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-25757356-1']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();
</script>
</head>
<body>

<?

$files = array();
$d = dir($location);
while (false !== ($entry = $d->read())) {
  if (!endsWith($entry, '.export')) {
    continue;
  }
  $files[] = $entry;
}
$d->close();

sort($files);

$titles = array();
$descriptions = array();
foreach ($files as $f) {
  $titles[$f] = preg_replace('/\.export$/', '', preg_replace('/_/',' ', preg_replace('/^[^_]*_/','' , $f)));
  $s = file_get_contents($location . $f);
  $ss = explode("\n", $s, 3);
  if (array_key_exists(1, $ss)) {
    $s = $ss[1];
  } else {
    $s = '';
  }
  $s = urldecode($s);
  $descriptions[$f] = $s;
}

?>

<h1>Import rule sets</h1>

<?

if ($isAndroid) {
  echo 'Import a rule set of your choice directly to your <a href="https://market.android.com/details?id=de.ub0r.android.callmeter">Call Meter 3G</a> simply by selecting its link below. To import the very same rule set on an other device, you should just open the barcode and scan it with a barcode scanner app (e.g. <a href="https://market.android.com/details?id=com.google.zxing.client.android">Barcode Scanner</a>).' . "<br />\n";
} else {
  echo 'Import a rule set of your choice directly to your <a href="https://market.android.com/details?id=de.ub0r.android.callmeter">Call Meter 3G</a> simply by scanning the barcode with a barcode scanner app (e.g. <a href="https://market.android.com/details?id=com.google.zxing.client.android">Barcode Scanner</a>).' . "<br />\n";
}

echo 'You might need to edit your very own limit or cost settings after importing the rule set.' . "<br />\n";
echo '<br />If you want your rule set shown here, you just need to export it to me &lt;android+callmeter@ub0r.de&gt;.';
?>

<h2>Table of Rule sets</h2>
The follwoing rule sets are available to import. Click on an item to view its details.
<ul>
<?
foreach ($files as $f) {
  $ds = explode("\n", $descriptions[$f]);
  $d = $ds[0];
  echo '<li><a href="#f_' . $f . '">' . $titles[$f] . '</a>: ' . $d . '</li>' . "\n";
}
?>
</ul>

<h2>Rule sets in detail</h2>

<?
$i = 0;
foreach ($files as $f) {
  echo '<div class="ruleset" id="f_' . $f . '">' . "\n";
  $furl = 'http://www.ub0r.de/android/callmeter/rulesets/#f_' . $f;
  $importurl = 'import://callmeter.android.ub0r.de/www.ub0r.de/android/callmeter/rulesets/' . $f;
  $barcodeurl_s = 'http://' . ($i % 10) . '.chart.apis.google.com/chart?chs=100x100&amp;cht=qr&amp;chl=import%3A%2F%2Fcallmeter.android.ub0r.de%2Fwww.ub0r.de%2Fandroid%2Fcallmeter%2Frulesets%2F' . $f;
  $barcodeurl_l = 'http://chart.apis.google.com/chart?chs=400x400&amp;cht=qr&amp;chl=import%3A%2F%2Fcallmeter.android.ub0r.de%2Fwww.ub0r.de%2Fandroid%2Fcallmeter%2Frulesets%2F' . $f;
  $hasLongDescr = file_exists($location . $f . '.descr');
  $extLink = '';
  if (file_exists($location . $f . '.link')) {
    $extLink = file_get_contents($location . $f . '.link');
  }
  if ($isAndroid) {
    echo '<h3><a class="hidelink" href="' . $furl . '">' . $titles[$f] . '</a></h3>' . "\n";
    echo nl2br($descriptions[$f]) . '<br /><br />' . "\n";
    echo 'import: ';
    echo '<a href="' . $importurl . '">by link</a>' . "\n";
    echo ' / ';
    echo '<a href="' . $barcodeurl_l . '">by barcode</a>' . "\n";
    if ($hasLongDescr || $extLink) {
    echo '<br />' . "\n" . 'additional information: ';
    }
    if ($hasLongDescr) {
      echo '<a href="./' . $f . '.descr">long description</a>' . "\n";
    }
    if ($extLink) {
     echo '<a href="' . $extLink . '">external link</a>' . "\n";
    }
  } else {
    echo '<div class="barcode">';
    echo '<a href="' . $barcodeurl_l . '">';
    echo '<img src="' . $barcodeurl_s . '" />';
    echo '</a>' . "\n";
    echo '</div>';
    echo '<div class="text">';
    echo '<h3><a  class="hidelink" href="' . $furl . '">' . $titles[$f] . '</a></h3>' . "\n";
    echo nl2br($descriptions[$f]) . '<br />' . "\n";
    if ($hasLongDescr || $extLink) {
    echo 'additional information: ';
    }
    if ($hasLongDescr) {
      echo '<a href="./' . $f . '.descr">long description</a>' . "\n";
    }
    if ($extLink) {
     echo '<a href="' . $extLink . '">external link</a>' . "\n";
    }
    echo '</div>';
    echo '<div class="endtext" />';
  }
  echo '</div>' . "\n";
  $i++;
}
?>

</body>
</html>

