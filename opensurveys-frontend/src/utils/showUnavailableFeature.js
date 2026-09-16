/**
 * Temporary "feature not yet available" tooltip for stubbed UI actions
 * (e.g. Google/GitHub login, Survey Editor, Settings).
 *
 * Positions a short-lived tip above the clicked element (or below if there
 * is no room), and ensures only one tip is visible at a time.
 */
import { tGlobal } from '../i18n'

let activeTip = null
let hideTimer = null
let removeTimer = null

/** Call as an onClick handler: showUnavailableFeature(event). */
export function showUnavailableFeature(event) {
  if (event?.preventDefault) event.preventDefault()

  const el = event?.currentTarget
  if (!el) return

  // Replace any existing tip so rapid clicks don't stack tooltips.
  if (hideTimer) clearTimeout(hideTimer)
  if (removeTimer) clearTimeout(removeTimer)
  if (activeTip) {
    activeTip.remove()
    activeTip = null
  }

  const tip = document.createElement('div')
  tip.className = 'feature-unavailable-tip'
  tip.setAttribute('role', 'status')
  tip.textContent = tGlobal('common.featureUnavailable')
  document.body.appendChild(tip)

  // Center horizontally on the trigger; prefer above, fall back below.
  const rect = el.getBoundingClientRect()
  const tipRect = tip.getBoundingClientRect()
  let left = rect.left + rect.width / 2 - tipRect.width / 2
  let top = rect.top - tipRect.height - 10

  left = Math.max(8, Math.min(left, window.innerWidth - tipRect.width - 8))
  if (top < 8) top = rect.bottom + 10

  tip.style.left = `${left + window.scrollX}px`
  tip.style.top = `${top + window.scrollY}px`

  requestAnimationFrame(() => tip.classList.add('is-visible'))

  activeTip = tip
  hideTimer = setTimeout(() => {
    tip.classList.remove('is-visible')
    removeTimer = setTimeout(() => {
      tip.remove()
      if (activeTip === tip) activeTip = null
    }, 180)
  }, 2000)
}
