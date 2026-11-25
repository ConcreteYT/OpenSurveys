/**
 * First-visit dialog on the home page. Stays until the visitor confirms.
 * Legal links leave this page without setting the flag, so the dialog
 * is back when they return and can still agree after reading.
 */
import React, { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLocale, useT, T, LOCALE_LABELS } from '../i18n'
import LanguageSelectButton from '../animations/LanguageSelectButton'

const STORAGE_KEY = 'opensurveys-demo-notice'

export function hasAcceptedDemoNotice() {
  try {
    return window.localStorage.getItem(STORAGE_KEY) === '1'
  } catch {
    return false
  }
}

export default function DemoNotice({ onAccepted }) {
  const t = useT()
  const { locale, cycleLocale } = useLocale()
  const dialogRef = useRef(null)
  const [open, setOpen] = useState(() => !hasAcceptedDemoNotice())

  useEffect(() => {
    if (!open) return undefined
    const dialog = dialogRef.current
    const confirm = dialog?.querySelector('[data-demo-confirm]')
    confirm?.focus()

    const onKeyDown = (event) => {
      if (event.key !== 'Tab' || !dialog) return
      const focusable = Array.from(dialog.querySelectorAll('a, button'))
      if (focusable.length === 0) return
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [open])

  if (!open) return null

  const accept = () => {
    try {
      window.localStorage.setItem(STORAGE_KEY, '1')
    } catch {
      // Private mode can block storage; closing still lets this visit continue.
    }
    setOpen(false)
    onAccepted?.()
  }

  return (
    <div className="demo-notice-backdrop">
      <div
        ref={dialogRef}
        className="demo-notice"
        role="dialog"
        aria-modal="true"
        aria-labelledby="demo-notice-title"
      >
        <div className="demo-notice-header">
          <h2 id="demo-notice-title" className="demo-notice-title">
            <T k="demoNotice.title" />
          </h2>
          <LanguageSelectButton
            locale={locale}
            onCycleLocale={cycleLocale}
            className="demo-notice-lang"
            label={t('nav.language', { label: LOCALE_LABELS[locale] || LOCALE_LABELS.en })}
          />
        </div>
        <p className="demo-notice-body">{t('demoNotice.body')}</p>
        <div className="demo-notice-links">
          <Link to="/impressum">{t('legal.impressum')}</Link>
          <Link to="/datenschutz">{t('legal.privacy')}</Link>
        </div>
        <button type="button" className="demo-notice-confirm" data-demo-confirm onClick={accept}>
          {t('demoNotice.agree')}
        </button>
      </div>
    </div>
  )
}
