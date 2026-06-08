const CACHE_NAME = 'velotrack-v15';
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

// â”€â”€ Notification persistante pendant l'enregistrement â”€â”€â”€â”€â”€â”€â”€â”€â”€
// L'app envoie un message {type:'REC_UPDATE', data:{...}} toutes les 15s
// Le SW affiche/met Ã  jour la notification sur l'Ã©cran de verrouillage

self.addEventListener('message', e => {
  if (!e.data) return;

  if (e.data.type === 'REC_UPDATE') {
    const { dist, elapsed, speed, power, elev, grade, paused } = e.data.data;
    const icon = paused ? 'â¸' : 'ðŸš´';
    const status = paused ? 'En pause' : 'En cours';

    const lines = [
      `${dist} km Â· ${elapsed}`,
      `${speed} km/h${power ? ' Â· ' + power + ' W' : ''}`,
      elev > 0 ? `â†‘ ${elev}m Â· pente ${grade}` : `pente ${grade}`,
    ].join('\n');

    self.registration.showNotification(`${icon} VeloTrack â€” ${status}`, {
      body: lines,
      icon: './icon-192.png',
      badge: './icon-192.png',
      tag: 'velotrack-recording',   // mÃªme tag = remplace la prÃ©cÃ©dente
      renotify: false,
      silent: true,                  // pas de son Ã  chaque update
      requireInteraction: true,      // reste visible sur l'Ã©cran de verrouillage
      actions: [
        { action: 'open', title: 'ðŸ“± Ouvrir' },
        { action: 'pause', title: paused ? 'â–¶ Reprendre' : 'â¸ Pause' },
      ],
      data: { url: self.registration.scope }
    });
  }

  if (e.data.type === 'REC_STOP') {
    // Ferme la notification Ã  l'arrÃªt
    self.registration.getNotifications({ tag: 'velotrack-recording' })
      .then(notifs => notifs.forEach(n => n.close()));
  }
});

// Tap sur la notification â†’ ouvre l'app sur l'onglet record
self.addEventListener('notificationclick', e => {
  e.notification.close();

  if (e.action === 'pause') {
    // Envoie un message Ã  l'app pour toggle pause
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