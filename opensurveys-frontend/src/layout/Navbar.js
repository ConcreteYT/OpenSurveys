/**
 * Minimal public navbar for `/home`: brand link + language, accent and theme toggles.
 */
import React from 'react'
import { Link } from 'react-router-dom';
import ThemeToggleButton from '../animations/ThemeToggleButton';
import AccentCycleButton from '../animations/AccentCycleButton';
import LanguageSelectButton from '../animations/LanguageSelectButton';
import { useT, LOCALE_LABELS } from '../i18n';

export default function Navbar({ isDarkMode, onToggleDarkMode, accent, onSetAccent, locale, onCycleLocale }) {
  const t = useT()

  return (
    <nav className="navbar bg-body-tertiary">
      <div className="container-fluid">
        <Link to="/home" className="navbar-brand">
          <img src="/logo.png" alt="OpenSurveys" className="navbar-logo" />
        </Link>
        <div className="d-flex align-items-center gap-2">
          <LanguageSelectButton
            locale={locale}
            onCycleLocale={onCycleLocale}
            label={t('nav.language', { label: LOCALE_LABELS[locale] || LOCALE_LABELS.en })}
          />
          <AccentCycleButton accent={accent} onSetAccent={onSetAccent} />
          <ThemeToggleButton isDarkMode={isDarkMode} onToggleDarkMode={onToggleDarkMode} />
        </div>
      </div>
    </nav>
  )
}
