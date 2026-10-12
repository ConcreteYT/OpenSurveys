/**
 * Authenticated welcome/dashboard page (`/login-home`).
 * Greets the user by name (resolved from localStorage), offers CTAs to
 * open the survey editor or the surveys list, and accepts an access code
 * that opens `/access?formId=...`.
 */
import React, { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { useT, T } from '../i18n'
import ScrambleText from '../animations/ScrambleText'

export default function Home() {
  const navigate = useNavigate()
  const t = useT()
  const codeLength = 8
  const [digits, setDigits] = useState(Array(codeLength).fill(''))
  const [error, setError] = useState('')
  const [isChecking, setIsChecking] = useState(false)
  const inputsRef = useRef([])

  /**
   * Resolves a display name from common localStorage keys or a stored user JSON.
   * Returns an empty string when nothing usable is found.
   */
  const getDisplayName = () => {
    if (typeof window === 'undefined') return ''

    const directValues = [
      window.localStorage.getItem('userName'),
      window.localStorage.getItem('username'),
      window.localStorage.getItem('name'),
      window.localStorage.getItem('fullName'),
      window.localStorage.getItem('firstName')
    ]

    for (const value of directValues) {
      if (value && value.trim()) {
        return value.trim()
      }
    }

    try {
      const storedUser = window.localStorage.getItem('user')
      if (storedUser) {
        const parsedUser = JSON.parse(storedUser)
        const nestedName = parsedUser?.name || parsedUser?.fullName || parsedUser?.firstName || parsedUser?.username
        if (nestedName && nestedName.trim()) {
          return nestedName.trim()
        }
      }
    } catch (parseError) {
      console.warn('Unable to parse stored user data.', parseError)
    }

    return ''
  }

  /** Joins digit boxes into a form id, verifies via API, then opens the survey. */
  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    const code = digits.join('').trim()
    if (!code) {
      setError(t('home.errorEmpty'))
      return
    }

    // Access codes are the form id (leading zeros allowed, e.g. 00000004 → 4).
    const formId = Number.parseInt(code, 10)
    if (!Number.isFinite(formId) || formId <= 0) {
      setError(t('home.errorInvalid'))
      return
    }

    setIsChecking(true)
    try {
      await api.getForm(formId)
      navigate(`/access?formId=${formId}`)
    } catch (err) {
      const message = err?.message || ''
      if (/not found|404/i.test(message)) {
        setError(t('home.errorNotFound'))
      } else {
        setError(message || t('home.errorVerify'))
      }
    } finally {
      setIsChecking(false)
    }
  }

  const displayName = getDisplayName()
  const welcomeName = displayName ? ` ${displayName}` : ''

  return (
    <div className="container">
      <div className="py-4">
        <section className="background-radial-gradient overflow-hidden">
          <style>{`
    .background-radial-gradient {
      background-color: #ffffff;
      background-image: none;
      box-shadow: 0 8px 40px rgba(0,0,0,0.12);
      border-radius: 16px;
      padding: 24px;
    }

    .bg-glass {
      background-color: hsla(0, 0%, 100%, 0.9) !important;
      backdrop-filter: saturate(200%) blur(25px);
    }

    .welcome-card {
      border: 1px solid color-mix(in srgb, var(--accent) 12%, transparent);
      box-shadow: 0 12px 35px color-mix(in srgb, var(--accent) 12%, transparent);
    }

    .code-group {
      display: flex;
      flex-wrap: nowrap;
      justify-content: center;
      gap: clamp(0.2rem, 1.2vw, 0.5rem);
      width: 100%;
      max-width: 100%;
    }
    .code-input {
      flex: 1 1 0;
      width: auto;
      min-width: 0;
      max-width: 48px;
      aspect-ratio: 1;
      height: auto;
      text-align: center;
      font-size: clamp(0.85rem, 2.8vw, 1.1rem);
      padding: 0.2rem;
      border-radius: 8px;
    }
  `}</style>

          <div className="container px-2 px-sm-4 py-3 py-md-5 px-md-5 my-3 my-md-5">
            <div className="row gx-lg-5 align-items-center gy-4">
              <div className="col-lg-6">
                <div className="card bg-glass welcome-card p-3 p-md-5 h-100">
                  <h1 className="display-5 fw-bold mb-3 welcome-heading">
                    <T k="dashboard.welcome" vars={{ name: welcomeName }} />
                  </h1>
                  <p className="lead text-muted mb-4">
                    {t('dashboard.blurb')}
                  </p>
                  <div className="d-flex flex-wrap gap-3 justify-content-center mt-4">
                    <button type="button" className="btn btn-primary btn-lg px-4" onClick={() => navigate('/editor')}>
                      <T k="dashboard.openEditor" />
                    </button>
                    <button type="button" className="btn btn-outline-primary btn-lg px-4" onClick={() => navigate('/surveys')}>
                      <T k="dashboard.viewSurveys" />
                    </button>
                  </div>
                </div>
              </div>

              <div className="col-lg-6">
                <div className="card bg-glass welcome-card p-3 p-md-4 h-100 d-flex flex-column justify-content-center align-items-center text-center">
                  <h2 className="fw-bold mb-2 section-heading">
                    <T k="home.accessTitle" />
                  </h2>
                  <p className="text-muted">
                    <T k="home.accessHint" />
                  </p>

                  <form onSubmit={handleSubmit} className="w-100 d-flex flex-column align-items-center">
                    <div className="mb-3 w-100">
                      <label className="form-label"><T k="home.accessCode" /></label>
                      <div className="code-group">
                        {digits.map((d, i) => (
                          <input
                            key={i}
                            ref={(el) => (inputsRef.current[i] = el)}
                            type="text"
                            inputMode="numeric"
                            maxLength={1}
                            className="form-control code-input"
                            aria-label={`${t('home.accessCode')} ${i + 1}`}
                            value={d}
                            onChange={(e) => {
                              setError('')
                              const val = e.target.value.replace(/[^0-9]/g, '')
                              if (!val) {
                                const newDigits = [...digits]
                                newDigits[i] = ''
                                setDigits(newDigits)
                                return
                              }
                              const newDigits = [...digits]
                              newDigits[i] = val.slice(-1)
                              setDigits(newDigits)
                              if (i < codeLength - 1) inputsRef.current[i + 1]?.focus()
                            }}
                            onKeyDown={(e) => {
                              if (e.key === 'Backspace') {
                                e.preventDefault()
                                const newDigits = [...digits]
                                if (digits[i]) {
                                  newDigits[i] = ''
                                  setDigits(newDigits)
                                } else if (i > 0) {
                                  inputsRef.current[i - 1]?.focus()
                                  newDigits[i - 1] = ''
                                  setDigits(newDigits)
                                }
                              } else if (e.key === 'ArrowLeft' && i > 0) {
                                inputsRef.current[i - 1]?.focus()
                              } else if (e.key === 'ArrowRight' && i < codeLength - 1) {
                                inputsRef.current[i + 1]?.focus()
                              }
                            }}
                          />
                        ))}
                      </div>
                      {error && (
                        <div className="alert alert-danger mt-3 mb-0 py-2" role="alert">{error}</div>
                      )}
                    </div>

                    <button type="submit" className="btn btn-primary px-4" disabled={isChecking}>
                      <ScrambleText text={isChecking ? t('home.checking') : t('home.access')} />
                    </button>
                  </form>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
