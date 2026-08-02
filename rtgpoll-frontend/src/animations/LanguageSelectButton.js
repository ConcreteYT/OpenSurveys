import React, { useEffect, useRef } from 'react'
import { animate } from 'animejs'
import { LOCALE_LABELS } from '../i18n'

/**
 * Compact EN/DE label that flips when the language changes.
 */
export function LanguageSelectIcon({ locale }) {
  const iconRef = useRef(null)
  const isFirstRender = useRef(true)

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false
      return
    }

    const el = iconRef.current
    if (!el) return

    animate(el, {
      rotateY: [90, 0],
      duration: 280,
      ease: 'out(2)',
    })
  }, [locale])

  return (
    <span
      ref={iconRef}
      className="language-select-icon"
      style={{
        display: 'inline-block',
        fontSize: '0.75rem',
        fontWeight: 700,
        letterSpacing: '0.04em',
        transformOrigin: 'center',
      }}
      aria-hidden="true"
    >
      {LOCALE_LABELS[locale] || LOCALE_LABELS.en}
    </span>
  )
}

/**
 * Icon-only language cycle button (English ↔ German).
 */
export default function LanguageSelectButton({
  locale,
  onCycleLocale,
  className = 'btn btn-sm btn-outline-secondary',
  style,
  label,
}) {
  const display = LOCALE_LABELS[locale] || LOCALE_LABELS.en
  const ariaLabel = label || `Language: ${display}. Click to switch.`

  return (
    <button
      type="button"
      className={className}
      onClick={onCycleLocale}
      aria-label={ariaLabel}
      title={ariaLabel}
      style={{ width: '40px', height: '40px', perspective: '600px', ...style }}
    >
      <LanguageSelectIcon locale={locale} />
    </button>
  )
}
