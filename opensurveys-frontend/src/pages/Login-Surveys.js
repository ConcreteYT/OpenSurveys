/**
 * Authenticated "Your surveys" page (`/surveys`).
 * Lists forms owned by the logged-in user, shows zero-padded access codes,
 * and links to fill out (`/access`) or view aggregated results.
 */
import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { useT, T } from '../i18n'

/** Form id as an 8-digit access code (e.g. 4 → "00000004"). */
function formatAccessCode(formId) {
  return String(formId).padStart(8, '0')
}

export default function LoginSurveys() {
  const navigate = useNavigate()
  const t = useT()
  const [forms, setForms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Load the current user's forms once on mount; ignore late responses after unmount.
  useEffect(() => {
    let cancelled = false

    async function loadForms() {
      setLoading(true)
      setError('')
      try {
        const data = await api.listMyForms()
        if (!cancelled) {
          setForms(Array.isArray(data) ? data : [])
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || t('surveys.loadError'))
          setForms([])
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    loadForms()
    return () => {
      cancelled = true
    }
  }, [t])

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

    .survey-access-code {
      font-variant-numeric: tabular-nums;
      letter-spacing: 0.06em;
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    }
  `}</style>

          <div className="container px-2 px-sm-4 py-3 py-md-5 px-md-5 my-3 my-md-5">
            <div className="card bg-glass welcome-card p-3 p-md-5">
              <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
                <div>
                  <h1 className="display-6 fw-bold mb-2 welcome-heading"><T k="surveys.title" /></h1>
                  <p className="lead text-muted mb-0">
                    <T k="surveys.blurb" />
                  </p>
                </div>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => navigate('/editor')}
                >
                  <T k="surveys.openEditor" />
                </button>
              </div>

              {loading && (
                <p className="text-muted mb-0" role="status">{t('surveys.loading')}</p>
              )}

              {!loading && error && (
                <div className="alert alert-danger mb-0" role="alert">
                  {error}
                </div>
              )}

              {!loading && !error && forms.length === 0 && (
                <div className="text-muted">
                  <p className="mb-3"><T k="surveys.empty" /></p>
                  <button
                    type="button"
                    className="btn btn-outline-primary"
                    onClick={() => navigate('/login-home')}
                  >
                    <T k="surveys.back" />
                  </button>
                </div>
              )}

              {!loading && !error && forms.length > 0 && (
                <div className="table-responsive">
                  <table className="table align-middle mb-0">
                    <thead>
                      <tr>
                        <th scope="col"><T k="surveys.colName" /></th>
                        <th scope="col"><T k="surveys.colAccess" /></th>
                        <th scope="col"><T k="surveys.colQuestions" /></th>
                        <th scope="col" className="text-end"><T k="surveys.colActions" /></th>
                      </tr>
                    </thead>
                    <tbody>
                      {forms.map((form) => {
                        const questionCount = Array.isArray(form.questions) ? form.questions.length : 0
                        const accessCode = formatAccessCode(form.id)
                        return (
                          <tr key={form.id}>
                            <td className="fw-semibold">{form.name || t('surveys.untitled')}</td>
                            <td>
                              <span className="survey-access-code">{accessCode}</span>
                            </td>
                            <td>{questionCount}</td>
                            <td className="text-end">
                              <div className="d-flex flex-wrap gap-2 justify-content-end">
                                <Link
                                  className="btn btn-sm btn-outline-primary"
                                  to={`/access?formId=${form.id}`}
                                >
                                  <T k="surveys.open" />
                                </Link>
                                <Link
                                  className="btn btn-sm btn-outline-secondary"
                                  to={`/access?formId=${form.id}&view=responses`}
                                >
                                  <T k="surveys.results" />
                                </Link>
                                <Link
                                  className="btn btn-sm btn-primary"
                                  to={`/editor/${form.id}`}
                                >
                                  <T k="surveys.edit" />
                                </Link>
                              </div>
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
