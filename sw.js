const CACHE_NAME = 'velotrack-v3';
const ASSETS = [
  './index.html',
  './manifest.json'
];

self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(ASSETS))
  );
  self.skipWaiting(); // force activation immédiate
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => {
        console.log('[SW] Suppression ancien cache:', k);
        return caches.delete(k);
      }))
    )
  );
  self.clients.claim(); // prend le contrôle immédiatement
});

self.addEventListener('fetch', e => {
  // Network-first pour index.html et manifest (toujours la version fraîche)
  if (e.request.url.endsWith('index.html') || e.request.url.endsWith('manifest.json')) {
    e.respondWith(
      fetch(e.request).catch(() => caches.match(e.request))
    );
    return;
  }
  // Cache-first pour les assets statiques (tuiles OSM, fonts, etc.)
  e.respondWith(
    caches.match(e.request).then(cached => cached || fetch(e.request))
  );
});