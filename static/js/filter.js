function arrayUnique(a) {
  return a.reduce(function(p, c) {
      if (p.indexOf(c) < 0) p.push(c);
      return p;
  }, []);
}

function getParameterByName(name) {
  name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
  var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
      results = regex.exec(location.search);
  return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

function updateProviderList(c, p) {
  $('#provider').empty();
  $('#provider').append('<option value="">Provider</option>');
  $('#provider').append('<option value="">[all]</option>');

  var providers = [];

  $('#table_of_rulesets tr').each(function(i){
    if (typeof $(this).attr('rel-country') == 'undefined') {
      return;
    }
    if (c == null || c == '' || c == $(this).attr('rel-country')) {
      providers.push($(this).attr('rel-provider'));
    }
  });

  providers = arrayUnique(providers.sort());

  $.each(providers, function(i,e){
    $('#provider').append('<option value="' + e + '"' + (e == p ? ' selected' : '') + '>' + e + '</option>');
  });
}

function setFilter(c, p) {
  $('#table_of_rulesets tr').each(function(i){
    if (typeof $(this).attr('rel-country') == 'undefined' || typeof $(this).attr('rel-provider') == 'undefined') {
      return;
    }
    countryMatch = c === '' || c == $(this).attr('rel-country');
    providerMatch = p === '' || p == $(this).attr('rel-provider');
    h = !countryMatch || !providerMatch;
    $(this).toggleClass('hidden', h);
  });

  updateProviderList(c, p);
}

function updateFilter() {
  var c = $('#country').val();
  var p = $('#provider').val();
  setFilter(c, p);
}

function setImportLinks() {
  if (navigator.userAgent.match(/Android/)) {
    return;
  }

  // change link to open popup
  var overlayconfig = {
    mask: { opacity: 0.8},
    onLoad: function() {
      $("img.lazy").lazyload({ effect: "fadeIn"});
    }
  };

  $("a[rel]").overlay(overlayconfig);
}

window.onload = function() {
  $('#country').change(updateFilter);
  $('#provider').change(updateFilter);

  var c = getParameterByName('country');
  var p = getParameterByName('provider');
  setFilter(c, p);

  setImportLinks();

  $("img.lazy").lazyload({ effect: "fadeIn"});
}
