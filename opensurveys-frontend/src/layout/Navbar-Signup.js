/**
 * Navbar for signup (`/auth/signup`) and the public survey access page (`/access`).
 * Brand link back to home, language/theme/accent toggles, and a Login link.
 */
import React from 'react'
import { Link } from 'react-router-dom';
import ThemeToggleButton from '../animations/ThemeToggleButton';
import AccentCycleButton from '../animations/AccentCycleButton';
import LanguageSelectButton from '../animations/LanguageSelectButton';
import { useT, LOCALE_LABELS, T } from '../i18n';

export default function NavbarSignup({ isDarkMode, onToggleDarkMode, accent, onSetAccent, locale, onCycleLocale }) {
  const t = useT()

  return (
    <div>
      <nav className="navbar navbar-expand-lg bg-body-tertiary">
        <div className="container-fluid">
          <Link to="/home" className="navbar-brand">
            <img src="/logo.png" alt="OpenSurveys" className="navbar-logo" />
          </Link>
          <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label={t('nav.toggle')}>
            <span className="navbar-toggler-icon"></span>
          </button>
          <div className="collapse navbar-collapse" id="navbarSupportedContent">
            <ul className="navbar-nav me-auto mb-2 mb-lg-0">
            </ul>
            <div className="d-flex align-items-center gap-2">
              <LanguageSelectButton
                locale={locale}
                onCycleLocale={onCycleLocale}
                label={t('nav.language', { label: LOCALE_LABELS[locale] || LOCALE_LABELS.en })}
              />
              <AccentCycleButton accent={accent} onSetAccent={onSetAccent} />
              <ThemeToggleButton isDarkMode={isDarkMode} onToggleDarkMode={onToggleDarkMode} />
              <Link to="/auth/login" className="btn btn-outline-success"><T k="nav.login" /></Link>
            </div>
          </div>
        </div>
      </nav>
    </div>
  )
}
