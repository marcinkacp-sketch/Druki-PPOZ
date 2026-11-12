const CACHE = 'druki-ppoz-v3';
const STATIC = ['./', './index.html', './manifest.webmanifest', './icons/icon-192.png', './icons/icon-512.png', './templates/noma2_blank.png'];
self.addEventListener('install',event=>{
 event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(STATIC)).then(()=>self.skipWaiting()));
});
self.addEventListener('activate',event=>{
 event.waitUntil(Promise.all([caches.keys().then(names=>Promise.all(names.filter(name=>name!==CACHE).map(name=>caches.delete(name)))),self.clients.claim()]));
});
self.addEventListener('fetch',event=>{
 const request=event.request;
 if(request.method!=='GET'||new URL(request.url).origin!==self.location.origin) return;
 event.respondWith(caches.match(request).then(cached=>cached||fetch(request)));
});