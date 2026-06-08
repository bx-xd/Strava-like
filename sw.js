const CACHE_NAME = 'velotrack-v12';
const ASSETS = ['./index.html', './manifest.json'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE_NAME).then(c => c.addAll(ASSETS)));
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    )
  );
  self.clients.claim();
});

self.addEventListener('fetch', e => {
  if (e.request.url.endsWith('index.html') || e.request.url.endsWith('manifest.json')) {
    e.respondWith(fetch(e.request).catch(() => caches.match(e.request)));
    return;
  }
  e.respondWith(caches.match(e.request).then(cached => cached || fetch(e.request)));
});

// ── Notification persistante pendant l'enregistrement ─────────
// L'app envoie un message {type:'REC_UPDATE', data:{...}} toutes les 15s
// Le SW affiche/met à jour la notification sur l'écran de verrouillage

self.addEventListener('message', e => {
  if (!e.data) return;

  if (e.data.type === 'REC_UPDATE') {
    const { dist, elapsed, speed, power, elev, grade, paused } = e.data.data;
    const icon = paused ? '⏸' : '🚴';
    const status = paused ? 'En pause' : 'En cours';

    const lines = [
      `${dist} km · ${elapsed}`,
      `${speed} km/h${power ? ' · ' + power + ' W' : ''}`,
      elev > 0 ? `↑ ${elev}m · pente ${grade}` : `pente ${grade}`,
    ].join('\n');

    self.registration.showNotification(`${icon} VeloTrack — ${status}`, {
      body: lines,
      icon: './icon-192.png',
      badge: './icon-192.png',
      tag: 'velotrack-recording',   // même tag = remplace la précédente
      renotify: false,
      silent: true,                  // pas de son à chaque update
      requireInteraction: true,      // reste visible sur l'écran de verrouillage
      actions: [
        { action: 'open', title: '📱 Ouvrir' },
        { action: 'pause', title: paused ? '▶ Reprendre' : '⏸ Pause' },
      ],
      data: { url: self.registration.scope }
    });
  }

  if (e.data.type === 'REC_STOP') {
    // Ferme la notification à l'arrêt
    self.registration.getNotifications({ tag: 'velotrack-recording' })
      .then(notifs => notifs.forEach(n => n.close()));
  }
});

// Tap sur la notification → ouvre l'app sur l'onglet record
self.addEventListener('notificationclick', e => {
  e.notification.close();

  if (e.action === 'pause') {
    // Envoie un message à l'app pour toggle pause
    e.waitUntil(
      self.clients.matchAll({ type:'window', includeUncontrolled:true }).then(clients => {
        if (clients.length > 0) {
          clients[0].postMessage({ type: 'NOTIF_PAUSE' });
          clients[0].focus();
        } else {
          self.clients.openWindow(self.registration.scope);
        }
      })
    );
    return;
  }

  // Action 'open' ou tap direct
  e.waitUntil(
    self.clients.matchAll({ type:'window', includeUncontrolled:true }).then(clients => {
      if (clients.length > 0) {
        clients[0].focus();
        clients[0].postMessage({ type: 'NOTIF_OPEN' });
      } else {
        self.clients.openWindow(self.registration.scope + '#record');
      }
    })
  );
});
