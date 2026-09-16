/**
 * Public landing page (`/home`).
 * Left: 8-digit access-code entry that verifies a form exists, then navigates
 * to `/access?formId=...`. Right: CTAs to login or sign up to create surveys.
 */
import React, { useState, useRef, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useT, T } from '../i18n';
import ScrambleText from '../animations/ScrambleText';

export default function Home() {
  const navigate = useNavigate()
  const t = useT()
  const codeLength = 8
  const [digits, setDigits] = useState(Array(codeLength).fill(''))
  const [error, setError] = useState('')
  const [isChecking, setIsChecking] = useState(false)
  const inputsRef = useRef([])

  // Focus the first digit box on mount for quick entry.
  useEffect(() => {
    if (inputsRef.current[0]) inputsRef.current[0].focus()
  }, [])

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

  return (
    <div className="container py-3 py-md-5">
      <section className="background-radial-gradient overflow-hidden">
        <style>{`
    .background-radial-gradient {
      position: relative;
      background-color: rgba(255, 255, 255, 0.18);
      background-image: none;
      backdrop-filter: saturate(160%) blur(28px);
      -webkit-backdrop-filter: saturate(160%) blur(28px);
      border: 4px solid rgba(255, 255, 255, 0.45);
      box-shadow: 0 8px 40px color-mix(in srgb, var(--accent) 8%, transparent);
      border-radius: 16px;
      padding: 32px;
      min-height: 80vh;
    }

    .bg-glass {
      background-color: rgba(255, 255, 255, 0.28) !important;
      backdrop-filter: saturate(180%) blur(24px);
      -webkit-backdrop-filter: saturate(180%) blur(12px);
      border: 1px solid rgba(255, 255, 255, 0.5);
      box-shadow: 0 8px 32px color-mix(in srgb, var(--accent) 6%, transparent);
    }

    .purple-heading {
      color: var(--accent);
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
    @media (max-width: 575.98px) {
      .background-radial-gradient {
        padding: 12px;
        min-height: auto;
        border-width: 2px;
      }
    }
  `}</style>

        <div className="container px-2 px-sm-4 py-3 py-md-5">
          <div className="row gy-4">

            <div className="col-lg-6">
              <div className="card bg-glass p-3 p-md-4 h-100 d-flex flex-column justify-content-center align-items-center text-center" style={{ zIndex: 10 }}>
                <h2 className="fw-bold purple-heading"><T k="home.accessTitle" /></h2>
                <p className="text-muted"><T k="home.accessHint" /></p>

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
                            if (i < codeLength - 1) inputsRef.current[i + 1].focus()
                          }}
                          onKeyDown={(e) => {
                            if (e.key === 'Backspace') {
                              e.preventDefault()
                              const newDigits = [...digits]
                              if (digits[i]) {
                                newDigits[i] = ''
                                setDigits(newDigits)
                              } else if (i > 0) {
                                inputsRef.current[i - 1].focus()
                                newDigits[i - 1] = ''
                                setDigits(newDigits)
                              }
                            } else if (e.key === 'ArrowLeft' && i > 0) {
                              inputsRef.current[i - 1].focus()
                            } else if (e.key === 'ArrowRight' && i < codeLength - 1) {
                              inputsRef.current[i + 1].focus()
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

            <div className="col-lg-6">
              <div className="card bg-glass p-3 p-md-4 h-100 d-flex flex-column justify-content-center align-items-center text-center">
                <div className="d-flex flex-column align-items-center" style={{ height: '100%', justifyContent: 'center' }}>
                  <h2 className="fw-bold purple-heading"><T k="home.createTitle" /></h2>
                  <p className="text-muted"><T k="home.createHint" /></p>
                  <div className="d-flex flex-wrap gap-2 justify-content-center" style={{ marginTop: '1.5rem' }}>
                    <Link to="/auth/login" className="btn btn-outline-primary">
                      <T k="home.login" />
                    </Link>
                    <Link to="/auth/signup" className="btn btn-primary">
                      <T k="home.signup" />
                    </Link>
                  </div>
                </div>
              </div>
            </div>

          </div>

          <div className="row">
            <div className="col-12">
              <div className="text-center my-3 my-md-5">
                <h1 className="my-3 my-md-5 display-5 fw-bold ls-tight welcome-heading">
                  <T k="home.taglineLine1" /><br />
                  <span style={{ display: 'block' }}><T k="home.taglineLine2" /></span>
                </h1>
              </div>
            </div>
          </div>

        </div>
      </section>
    </div>
  )
}
