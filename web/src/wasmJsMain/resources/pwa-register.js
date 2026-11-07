// Registers the service worker (sw.js, generated at build time), asks the browser to keep this origin's storage
// (installed iOS web apps are exempt from Safari's 7-day eviction; elsewhere persist() may be declined) and reloads once
// when a new build takes over, unless a workout is in progress (then the next start picks it up).
(function () {
  try { if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(function () {}); } catch (e) {}
  if (!('serviceWorker' in navigator)) return;
  var hadController = !!navigator.serviceWorker.controller;
  var reloading = false;
  navigator.serviceWorker.addEventListener('controllerchange', function () {
    if (!hadController || reloading) { hadController = true; return; }
    var busy = false;
    try { for (var i = 0; i < localStorage.length; i++) { var k = localStorage.key(i); if (k && k.indexOf('workout_checkpoint') >= 0 && localStorage.getItem(k)) busy = true; } } catch (e) {}
    if (busy) return;
    reloading = true;
    location.reload();
  });
  window.addEventListener('load', function () {
    navigator.serviceWorker.register('sw.js', { updateViaCache: 'none' }).catch(function (e) { console.warn('service worker not registered', e); });
  });
})();
