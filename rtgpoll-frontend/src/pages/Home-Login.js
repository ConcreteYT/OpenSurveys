/**
 * Login page (`/auth/login`): username/password form that stores the JWT and
 * redirects to the authenticated dashboard. Google Sign-In is wired; GitHub is a stub.
 */
import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { requestGoogleIdToken } from '../auth/googleSignIn';
import { showUnavailableFeature } from '../utils/showUnavailableFeature';
import { useT, T } from '../i18n';
import ScrambleText from '../animations/ScrambleText';

export default function Home() {
  const navigate = useNavigate();
  const t = useT()
  const [showPassword, setShowPassword] = useState(false)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const finishAuth = (token, role, authUsername) => {
    localStorage.setItem('token', token)
    localStorage.setItem('username', authUsername)
    localStorage.setItem('role', role || 'USER')
    navigate(role === 'ADMIN' ? '/admin' : '/login-home')
  }

  /** POST /auth/login → save token + username → navigate to /login-home. */
  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      const { token, role, username: authUsername } = await api.login({ username, password })
      finishAuth(token, role, authUsername || username)
    } catch (err) {
      setError(err.message || t('login.error'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleGoogle = async () => {
    setError('')
    setIsSubmitting(true)
    try {
      const idToken = await requestGoogleIdToken()
      const { token, role, username: authUsername } = await api.googleLogin({ idToken })
      finishAuth(token, role, authUsername)
    } catch (err) {
      setError(err.message || t('login.error'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="container">
      <div className="py-4">
        <section className="background-radial-gradient overflow-hidden">
          <style>{`
    .background-radial-gradient {
      background-color: rgba(255, 255, 255, 0.12);
      background-image: none;
      backdrop-filter: saturate(160%) blur(28px);
      -webkit-backdrop-filter: saturate(160%) blur(28px);
      border: 1px solid rgba(255, 255, 255, 0.28);
      box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      padding: 24px;
    }

    .bg-glass {
      background-color: rgba(255, 255, 255, 0.22) !important;
      backdrop-filter: saturate(180%) blur(24px);
      -webkit-backdrop-filter: saturate(180%) blur(24px);
      border: 1px solid rgba(255, 255, 255, 0.35);
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
    }
  `}</style>

          <div className="container px-2 px-sm-4 py-3 py-md-5 px-md-5 text-center text-lg-start my-3 my-md-5">
            <div className="row gx-lg-5 align-items-center mb-3 mb-md-5">
              <div className="col-lg-6 mb-4 mb-lg-0 d-flex flex-column justify-content-center align-items-center text-center" style={{ zIndex: 10 }}>
                <h1 className="my-3 my-md-5 display-5 fw-bold ls-tight welcome-heading">
                  <T k="home.taglineLine1" /><br />
                  <span style={{ display: 'block' }}><T k="home.taglineLine2" /></span>
                </h1>
              </div>

              <div className="col-lg-6 mb-4 mb-lg-0 position-relative">
                <div className="card bg-glass">
                  <div className="card-body px-3 py-4 px-md-5 py-md-5">
                    {error && (
                      <div className="alert alert-danger" role="alert">{error}</div>
                    )}
                    <form onSubmit={handleSubmit}>

                      <div data-mdb-input-init className="form-outline mb-4">
                        <input
                          type="text"
                          id="form3Example3"
                          className="form-control"
                          value={username}
                          onChange={(e) => setUsername(e.target.value)}
                          required
                        />
                        <label className="form-label" htmlFor="form3Example3"><T k="login.username" /></label>
                      </div>

                      <div className="form-outline mb-4">
                        <div className="input-group">
                          <input
                            type={showPassword ? 'text' : 'password'}
                            id="form3Example4"
                            className="form-control"
                            aria-label={t('login.password')}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                          />
                          <button
                            type="button"
                            className="btn btn-outline-secondary"
                            onClick={() => setShowPassword(!showPassword)}
                            aria-label={showPassword ? t('common.hidePassword') : t('common.showPassword')}
                            style={{ color: 'black', minWidth: 42, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                          >
                            {showPassword ? (
                              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="black" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                <circle cx="12" cy="12" r="3" />
                                <path d="M4 4l16 16" />
                              </svg>
                            ) : (
                              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="black" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                <circle cx="12" cy="12" r="3" />
                              </svg>
                            )}
                          </button>
                        </div>
                        <label className="form-label" htmlFor="form3Example4"><T k="login.password" /></label>
                      </div>

                      <div className="d-flex flex-column align-items-center gap-3 mb-3" style={{ marginTop: 8 }}>
                        <button
                          type="submit"
                          data-mdb-button-init
                          data-mdb-ripple-init
                          className="btn btn-primary login"
                          disabled={isSubmitting}
                        >
                          <ScrambleText text={isSubmitting ? t('login.submitting') : t('login.submit')} />
                        </button>
                        <div className="text-center">
                          <div className="mb-0"><T k="login.orWith" /></div>
                          <div className="d-flex flex-wrap gap-2 justify-content-center mt-2">
                            <button type="button" className="btn btn-outline-danger px-4" onClick={handleGoogle} disabled={isSubmitting}>
                              Google
                            </button>
                            <button type="button" className="btn btn-outline-dark px-4" onClick={showUnavailableFeature}>
                              GitHub
                            </button>
                          </div>
                        </div>
                      </div>

                      <div className="text-center">
                        <button type="button" data-mdb-button-init data-mdb-ripple-init className="btn btn-link btn-floating mx-1">
                          <i className="fab fa-facebook-f"></i>
                        </button>

                        <button type="button" data-mdb-button-init data-mdb-ripple-init className="btn btn-link btn-floating mx-1">
                          <i className="fab fa-google"></i>
                        </button>

                        <button type="button" data-mdb-button-init data-mdb-ripple-init className="btn btn-link btn-floating mx-1">
                          <i className="fab fa-twitter"></i>
                        </button>

                        <button type="button" data-mdb-button-init data-mdb-ripple-init className="btn btn-link btn-floating mx-1">
                          <i className="fab fa-github"></i>
                        </button>
                      </div>

                      <div className="text-center mt-3">
                        <span><T k="login.noAccount" /> </span>
                        <Link
                          to="/auth/signup"
                          className="btn btn-link p-0 mb-1 text-primary fw-bold text-decoration-none auth-switch-link"
                          data-no-hover-anim>
                          <T k="login.signUp" />
                        </Link>
                      </div>
                    </form>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
