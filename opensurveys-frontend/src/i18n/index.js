/**
 * Lightweight i18n: locale state, persistence, and a t() helper.
 * Default UI locale is German; English is the fallback for missing keys.
 */
import React, { useCallback, useContext, useMemo } from 'react'
import {
  DEFAULT_LOCALE,
  FALLBACK_LOCALE,
  LOCALES,
  LOCALE_LABELS,
  translations,
} from './translations'
import { LocaleContext } from './context'

/** Module-level locale for non-React helpers (e.g. showUnavailableFeature). */
let activeLocale = DEFAULT_LOCALE

export function getActiveLocale() {
  return activeLocale
}

export function setActiveLocale(locale) {
  activeLocale = LOCALES.includes(locale) ? locale : DEFAULT_LOCALE
}

function interpolate(template, vars) {
  if (!vars || typeof template !== 'string') return template
  return template.replace(/\{(\w+)\}/g, (_, name) =>
    vars[name] != null ? String(vars[name]) : `{${name}}`
  )
}

/** Translate a key for the given locale (falls back to English, then the key). */
export function translate(locale, key, vars) {
  const dict = translations[locale] || translations[DEFAULT_LOCALE]
  const fallback = translations[FALLBACK_LOCALE]
  const template = dict[key] ?? fallback[key] ?? key
  return interpolate(template, vars)
}

export function tGlobal(key, vars) {
  return translate(activeLocale, key, vars)
}

export function LocaleProvider({ locale, setLocale, children }) {
  const cycleLocale = useCallback(() => {
    setLocale((prev) => {
      const idx = LOCALES.indexOf(prev)
      return LOCALES[(idx + 1) % LOCALES.length]
    })
  }, [setLocale])

  const t = useCallback(
    (key, vars) => translate(locale, key, vars),
    [locale]
  )

  const value = useMemo(
    () => ({ locale, setLocale, cycleLocale, t }),
    [locale, setLocale, cycleLocale, t]
  )

  return (
    <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>
  )
}

export function useLocale() {
  return useContext(LocaleContext)
}

export function useT() {
  return useContext(LocaleContext).t
}

export { LOCALES, DEFAULT_LOCALE, LOCALE_LABELS }
export { T } from './T'
