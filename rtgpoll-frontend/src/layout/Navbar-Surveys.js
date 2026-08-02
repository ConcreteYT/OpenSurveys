/**
 * Navbar for the surveys dashboard (`/surveys`).
 * Same account/logout pattern as Navbar-Home, plus a search input (not yet wired)
 * and a link to the Survey Editor.
 */
import React from 'react'
import { Link, useNavigate } from 'react-router-dom';
import { logout, isAdmin } from '../api/client';
import { ThemeToggleIcon } from '../animations/ThemeToggleButton';
import AccentCycleButton, { AccentMenuItems } from '../animations/AccentCycleButton';
import LanguageSelectButton from '../animations/LanguageSelectButton';
import { useT, LOCALE_LABELS, T } from '../i18n';
import ScrambleText from '../animations/ScrambleText';

export default function NavbarSurveys({ isDarkMode, onToggleDarkMode, accent, onSetAccent, locale, onCycleLocale }) {
  const navigate = useNavigate();
  const t = useT()

  /** Clears JWT/local auth state and returns to the public home page. */
  function handleLogout() {
    logout();
    navigate('/home', { replace: true });
  }

  const accountLabel =
    window.localStorage.getItem('userName') ||
    window.localStorage.getItem('username') ||
    t('nav.account')

  return (
    <nav className="navbar navbar-expand-lg bg-body-tertiary">
      <div className="container-fluid">
        <Link to="/login-home" className="navbar-brand">
          <img src="/logo.png" alt="RTGpoll" className="navbar-logo" />
        </Link>

        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarSupportedContent"
          aria-controls="navbarSupportedContent"
          aria-expanded="false"
          aria-label={t('nav.toggle')}
        >
          <span className="navbar-toggler-icon"></span>
        </button>

        <div className="collapse navbar-collapse" id="navbarSupportedContent">
          <ul className="navbar-nav me-auto mb-2 mb-lg-0">
            <li className="nav-item">
              <Link to="/surveys" className="nav-link active me-2">
                <T k="nav.surveys" />
              </Link>
            </li>
            {isAdmin() && (
              <li className="nav-item">
                <Link to="/admin" className="nav-link me-2">
                  <T k="nav.admin" />
                </Link>
              </li>
            )}
            <li className="nav-item">
              <Link to="/editor" className="nav-link me-2">
                <T k="nav.editor" />
              </Link>
            </li>
          </ul>

          <div className="d-flex align-items-center gap-2">
            <form className="d-flex" role="search">
              <input
                className="form-control me-2"
                type="search"
                placeholder={t('nav.searchSurveys')}
                aria-label={t('nav.search')}
              />
            </form>
            <LanguageSelectButton
              locale={locale}
              onCycleLocale={onCycleLocale}
              label={t('nav.language', { label: LOCALE_LABELS[locale] || LOCALE_LABELS.en })}
            />
            <AccentCycleButton accent={accent} onSetAccent={onSetAccent} />
            <details className="dropdown" style={{ position: 'relative' }}>
              <summary className="btn btn-outline-secondary" style={{ listStyle: 'none', cursor: 'pointer' }}>
                👤 <ScrambleText text={accountLabel} />
              </summary>
              <ul className="dropdown-menu" style={{ display: 'block', minWidth: '10rem', position: 'absolute', right: 0, left: 'auto' }}>
                <li>
                  <button
                    type="button"
                    className="dropdown-item"
                    onClick={onToggleDarkMode}
                    style={{ perspective: '600px' }}
                  >
                    <ThemeToggleIcon isDarkMode={isDarkMode} />{' '}
                    <ScrambleText text={isDarkMode ? t('nav.lightMode') : t('nav.darkMode')} />
                  </button>
                </li>
                <li><hr className="dropdown-divider" /></li>
                <AccentMenuItems accent={accent} onSetAccent={onSetAccent} />
                <li><hr className="dropdown-divider" /></li>
                <li>
                  <button type="button" className="dropdown-item" onClick={() => navigate('/settings')}>
                    <T k="nav.settings" />
                  </button>
                </li>
                <li><hr className="dropdown-divider" /></li>
                <li>
                  <button type="button" className="dropdown-item" onClick={handleLogout}>
                    <T k="nav.logout" />
                  </button>
                </li>
              </ul>
            </details>
          </div>
        </div>
      </div>
    </nav>
  )
}
