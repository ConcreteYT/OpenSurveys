import { GOOGLE_CLIENT_ID } from '../config/google'

/**
 * Shows a small overlay with Google's official Sign-In button and resolves
 * with the ID token (credential JWT) when the user completes sign-in.
 */
export function requestGoogleIdToken() {
  return new Promise((resolve, reject) => {
    if (!GOOGLE_CLIENT_ID) {
      reject(new Error('Set REACT_APP_GOOGLE_CLIENT_ID in rtgpoll-frontend/.env'))
      return
    }

    const start = () => {
      if (!window.google?.accounts?.id) {
        reject(new Error('Google Sign-In failed to load'))
        return
      }

      const overlay = document.createElement('div')
      overlay.setAttribute('role', 'dialog')
      overlay.setAttribute('aria-label', 'Google Sign-In')
      overlay.style.cssText = [
        'position:fixed',
        'inset:0',
        'z-index:10000',
        'display:flex',
        'align-items:center',
        'justify-content:center',
        'background:rgba(0,0,0,0.45)',
      ].join(';')

      const panel = document.createElement('div')
      panel.style.cssText = [
        'background:#fff',
        'color:#111',
        'border-radius:12px',
        'padding:24px',
        'min-width:280px',
        'box-shadow:0 12px 40px rgba(0,0,0,0.25)',
        'display:flex',
        'flex-direction:column',
        'align-items:center',
        'gap:16px',
      ].join(';')

      const title = document.createElement('div')
      title.textContent = 'Continue with Google'
      title.style.cssText = 'font-weight:600;font-size:1.05rem'

      const buttonHost = document.createElement('div')

      const cancel = document.createElement('button')
      cancel.type = 'button'
      cancel.textContent = 'Cancel'
      cancel.style.cssText = [
        'border:none',
        'background:transparent',
        'color:#555',
        'cursor:pointer',
        'padding:4px 8px',
      ].join(';')

      let settled = false
      const cleanup = () => {
        overlay.remove()
        document.removeEventListener('keydown', onKeyDown)
      }

      const fail = (message) => {
        if (settled) return
        settled = true
        cleanup()
        reject(new Error(message))
      }

      const succeed = (credential) => {
        if (settled) return
        settled = true
        cleanup()
        resolve(credential)
      }

      const onKeyDown = (event) => {
        if (event.key === 'Escape') fail('Google Sign-In was cancelled')
      }

      cancel.addEventListener('click', () => fail('Google Sign-In was cancelled'))
      overlay.addEventListener('click', (event) => {
        if (event.target === overlay) fail('Google Sign-In was cancelled')
      })
      document.addEventListener('keydown', onKeyDown)

      panel.appendChild(title)
      panel.appendChild(buttonHost)
      panel.appendChild(cancel)
      overlay.appendChild(panel)
      document.body.appendChild(overlay)

      window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: (response) => {
          if (response?.credential) {
            succeed(response.credential)
          } else {
            fail('Google Sign-In was cancelled')
          }
        },
        auto_select: false,
        cancel_on_tap_outside: true,
      })

      window.google.accounts.id.renderButton(buttonHost, {
        type: 'standard',
        theme: 'outline',
        size: 'large',
        text: 'continue_with',
        shape: 'rectangular',
        width: 280,
      })
    }

    if (window.google?.accounts?.id) {
      start()
      return
    }

    let tries = 0
    const timer = setInterval(() => {
      tries += 1
      if (window.google?.accounts?.id) {
        clearInterval(timer)
        start()
      } else if (tries > 50) {
        clearInterval(timer)
        reject(new Error('Google Sign-In failed to load'))
      }
    }, 100)
  })
}
