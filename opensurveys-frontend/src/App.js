/**
 * App root: React Router, theme/accent/locale persistence, and route → navbar + page wiring.
 *
 * Each entry in `windowOptions` pairs a path with a layout (navbar) and page component.
 * Flags: `protected` requires a valid JWT; `guestOnly` redirects logged-in users away.
 */
import React, { useEffect, useState, useCallback } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import './App.css';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';
import { isAuthenticated, isAdmin } from './api/client';
import Navbar from './layout/Navbar'; 
import NavbarHome from './layout/Navbar-Home';
import NavbarLogin from './layout/Navbar-Login';
import NavbarSignup from './layout/Navbar-Signup';
import NavbarSurveys from './layout/Navbar-Surveys';
import NavbarEditor from './layout/Navbar-Editor';
import NavbarAdmin from './layout/Navbar-Admin';
import HomePage from './pages/Home';
import HomeLoginPage from './pages/Home-Login';
import HomeSignupPage from './pages/Home-Signup';
import LoginHomePage from './pages/Login-Home';
import LoginSurveysPage from './pages/Login-Surveys';
import SurveyEditorPage from './pages/SurveyEditor';
import AdminPage from './pages/Admin';
import Access from './pages/access';
import SettingsPage from './pages/Settings';
import { runThemeWipe } from './animations/themeWipe';
import { ACCENT_COLORS } from './animations/AccentCycleButton';
import PersistentBlobs from './animations/PersistentBlobs';
import ButtonHover from './animations/ButtonHover';
import PageEnter from './animations/PageEnter';
import {
  LocaleProvider,
  DEFAULT_LOCALE,
  LOCALES,
  setActiveLocale,
} from './i18n';

const ACCENT_SET = new Set(ACCENT_COLORS);
const LOCALE_SET = new Set(LOCALES);

/** Declarative route table: path + navbar layout + page + auth flags. */
const windowOptions = [
  { id: 'access', label: 'Access + NavbarSignup', path: '/access', layout: NavbarSignup, page: Access,protected:false },
  { id: 'home', label: 'Navbar + Home', path: '/home', layout: Navbar, page: HomePage,protected:false },
  { id: 'login', label: 'Navbar Login + Home Login', path: '/auth/login', layout: NavbarLogin, page: HomeLoginPage,protected:false, guestOnly:true },
  { id: 'signup', label: 'Navbar Signup + Home Signup', path: '/auth/signup', layout: NavbarSignup, page: HomeSignupPage,protected:false, guestOnly:true },
  { id: 'login-home', label: 'Navbar Home + Login Home', path: '/login-home', layout: NavbarHome, page: LoginHomePage,protected:true }, 
  { id: 'surveys', label: 'Navbar Surveys + Login Surveys', path: '/surveys', layout: NavbarSurveys, page: LoginSurveysPage,protected:true },
  { id: 'editor', label: 'Navbar Editor + Survey Editor', path: '/editor', layout: NavbarEditor, page: SurveyEditorPage, protected: true },
  { id: 'editor-edit', label: 'Navbar Editor + Survey Editor Edit', path: '/editor/:formId', layout: NavbarEditor, page: SurveyEditorPage, protected: true },
  { id: 'admin', label: 'Navbar Admin + Admin', path: '/admin', layout: NavbarAdmin, page: AdminPage, protected: true, adminOnly: true },
  { id: 'settings', label: 'Navbar Home + Settings', path: '/settings', layout: NavbarHome, page: SettingsPage, protected: true },
];

/** Renders children only when a valid JWT exists; otherwise redirects to /home. */
const ProtectedRoute = ({ guest }) => {
  // Uses shared isAuthenticated() so expired/malformed JWTs are cleared and
  // treated as logged-out, matching what api/client.js attaches as Bearer token.
  if (!isAuthenticated()) {
    return <Navigate to="/home" replace />;
  }
  return guest;
};

/** Renders children only for admins; non-admins go to dashboard, guests to /home. */
const AdminRoute = ({ guest }) => {
  if (!isAuthenticated()) {
    return <Navigate to="/home" replace />;
  }
  if (!isAdmin()) {
    return <Navigate to="/login-home" replace />;
  }
  return guest;
};

/** Renders children only for logged-out users; otherwise redirects to /login-home. */
const GuestRoute = ({ guest }) => {
  if (isAuthenticated()) {
    return <Navigate to="/login-home" replace />;
  }
  return guest;
};

function App() {
  // Theme, accent, and locale are restored from localStorage so preferences survive reloads.
  const [isDarkMode, setIsDarkMode] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.localStorage.getItem('rtg-theme') === 'dark';
  });

  const [accent, setAccent] = useState(() => {
    if (typeof window === 'undefined') return 'green';
    const stored = window.localStorage.getItem('rtg-accent');
    return ACCENT_SET.has(stored) ? stored : 'green';
  });

  const [locale, setLocale] = useState(() => {
    if (typeof window === 'undefined') return DEFAULT_LOCALE;
    const stored = window.localStorage.getItem('rtg-locale');
    return LOCALE_SET.has(stored) ? stored : DEFAULT_LOCALE;
  });

  // Sync dark/light mode to Bootstrap theme attrs, body styles, and localStorage.
  useEffect(() => {
    const theme = isDarkMode ? 'dark' : 'light';
    document.documentElement.setAttribute('data-bs-theme', theme);
    document.body.setAttribute('data-theme', theme);
    document.body.style.backgroundColor = isDarkMode ? '#121212' : '#f7f7fb';
    document.body.style.color = isDarkMode ? '#f8f9fa' : '#212529';
    window.localStorage.setItem('rtg-theme', theme);
  }, [isDarkMode]);

  // Sync accent CSS variable (data-accent) and persist the choice.
  useEffect(() => {
    document.documentElement.setAttribute('data-accent', accent);
    window.localStorage.setItem('rtg-accent', accent);
  }, [accent]);

  // Sync UI language (html lang + localStorage + non-React helpers).
  useEffect(() => {
    document.documentElement.setAttribute('lang', locale);
    window.localStorage.setItem('rtg-locale', locale);
    setActiveLocale(locale);
  }, [locale]);

  // Theme/accent changes run through a circular wipe animation when supported.
  const handleToggleDarkMode = useCallback((event) => {
    runThemeWipe({
      event,
      onApply: () => setIsDarkMode((prev) => !prev),
    });
  }, []);

  const handleSetAccent = useCallback((nextAccent, event) => {
    if (!ACCENT_SET.has(nextAccent)) return;
    runThemeWipe({
      event,
      onApply: () => setAccent(nextAccent),
    });
  }, []);

  const handleCycleLocale = useCallback(() => {
    setLocale((prev) => {
      const idx = LOCALES.indexOf(prev);
      return LOCALES[(idx + 1) % LOCALES.length];
    });
  }, []);

  return (
    <LocaleProvider locale={locale} setLocale={setLocale}>
      <Router>
        <div className={`App ${isDarkMode ? 'theme-dark' : 'theme-light'} accent-${accent}`}>
          {/* Global decorative blobs + button hover; render once for the whole app. */}
          <PersistentBlobs />
          <ButtonHover />
          <div className="app-shell">
            <Routes>
              <Route path="/" element={<Navigate to="/home" replace />} />

              {windowOptions.map((option) => {
                const Layout = option.layout;
                const Page = option.page;

                // Each route renders its navbar + page; PageEnter animates content on enter.
                const guest = (
                  <>
                    <Layout
                      isDarkMode={isDarkMode}
                      onToggleDarkMode={handleToggleDarkMode}
                      accent={accent}
                      onSetAccent={handleSetAccent}
                      locale={locale}
                      onCycleLocale={handleCycleLocale}
                    />
                    <PageEnter key={option.path}>
                      <Page />
                    </PageEnter>
                  </>
                );

                let element = guest;
                if (option.adminOnly) {
                  element = <AdminRoute guest={guest} />;
                } else if (option.protected) {
                  element = <ProtectedRoute guest={guest} />;
                } else if (option.guestOnly) {
                  element = <GuestRoute guest={guest} />;
                }

                return (
                  <Route
                    key={option.id}
                    path={option.path}
                    element={element}
                  />
                );
              })}
            </Routes>
          </div>
        </div>
      </Router>
    </LocaleProvider>
  );
}

export default App;
