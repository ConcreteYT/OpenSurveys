/**
 * Locale React context (kept separate so T / Scramble helpers can import
 * hooks without circular deps through index.js).
 */
import { createContext } from 'react'
import { DEFAULT_LOCALE } from './translations'

export const LocaleContext = createContext({
  locale: DEFAULT_LOCALE,
  setLocale: () => {},
  cycleLocale: () => {},
  t: (key) => key,
})
