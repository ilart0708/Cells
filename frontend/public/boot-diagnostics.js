// Loaded before the app. If the app fails to start (a script error, a script the phone's browser engine cannot
// read, or a failed load), this puts a readable message on the screen instead of leaving a blank white page.
// It is a separate file, not inline, because the website's Content-Security-Policy does not allow inline scripts.
;(function () {
  function show(message) {
    if (window.__cellsMounted) return // the app is running: leave its own error handling alone
    var root = document.getElementById('app')
    if (!root || root.getAttribute('data-boot-error')) return
    root.setAttribute('data-boot-error', 'true')

    var box = document.createElement('div')
    box.style.cssText = 'font-family:system-ui,sans-serif;max-width:480px;margin:0 auto;padding:32px 20px;color:#0E2250'

    var title = document.createElement('h1')
    title.textContent = 'CELLS could not start'
    title.style.cssText = 'font-size:22px;margin:0 0 12px'

    var help = document.createElement('p')
    help.textContent =
      'Close the app and open it again. If this keeps happening, update "Android System WebView" and "Google Chrome" in the Play Store, then try again.'
    help.style.cssText = 'font-size:16px;line-height:1.5;margin:0 0 16px'

    var detail = document.createElement('pre')
    detail.textContent = String(message || 'Unknown error').slice(0, 600)
    detail.style.cssText =
      'font-size:13px;line-height:1.4;color:#47557A;white-space:pre-wrap;word-break:break-word;background:#F4F6FB;border:1px solid #DCE3F2;border-radius:12px;padding:12px;margin:0'

    box.appendChild(title)
    box.appendChild(help)
    box.appendChild(detail)
    root.innerHTML = ''
    root.appendChild(box)
  }

  window.__cellsBootError = show

  // Script errors, including a script that failed to load (capture phase reaches errors that do not bubble).
  window.addEventListener(
    'error',
    function (event) {
      var target = event.target
      if (target && target !== window && target.tagName === 'SCRIPT') {
        show('Could not load ' + (target.src || 'a script') + '.')
      } else {
        show(event.message || (event.error && event.error.message))
      }
    },
    true,
  )

  window.addEventListener('unhandledrejection', function (event) {
    var reason = event.reason
    show((reason && reason.message) || reason)
  })
})()
