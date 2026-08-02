/**
 * Authenticated welcome/dashboard page (`/login-home`).
 * Greets the user by name (resolved from localStorage) and offers CTAs to
 * open the survey editor or navigate to the surveys list.
 */
import React from 'react'
import { useNavigate } from 'react-router-dom'
import { useT, T } from '../i18n'

export default function Home() {
  const navigate = useNavigate()
  const t = useT()

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
    } catch (error) {
      console.warn('Unable to parse stored user data.', error)
    }

    return ''
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
  `}</style>

          <div className="container px-2 px-sm-4 py-3 py-md-5 px-md-5 my-3 my-md-5">
            <div className="row gx-lg-5 align-items-center">
              <div className="col-lg-7 mb-4 mb-lg-0">
                <div className="card bg-glass welcome-card p-3 p-md-5">
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

              <div className="col-lg-5 position-relative">
                <div className="card bg-glass welcome-card p-3 p-md-5 position-relative">
                  <h2 className="fw-bold mb-3 section-heading">
                    <T k="dashboard.nextTitle" />
                  </h2>
                  <ul className="list-unstyled text-muted mb-0">
                    <li className="mb-2">• <T k="dashboard.next1" /></li>
                    <li className="mb-2">• <T k="dashboard.next2" /></li>
                    <li>• <T k="dashboard.next3" /></li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
