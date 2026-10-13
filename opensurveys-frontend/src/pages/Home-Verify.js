/**
 * Email verification after signup (`/auth/verify`).
 */
import React, { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { api, markSession } from '../api/client'
import { useT, T } from '../i18n'
import ScrambleText from '../animations/ScrambleText'

export default function VerifyEmail() {
  const t = useT()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState(location.state?.username || '')
  const [code, setCode] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setMessage('')
    setIsSubmitting(true)
    try {
      const result = await api.verifyEmail({ username: username.trim(), code: code.trim() })
      markSession(result.role || 'USER', result.username || username.trim())
      navigate(result.role === 'ADMIN' ? '/admin' : '/login-home')
    } catch (err) {
      setError(err.message || t('verify.error'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleResend = async () => {
    setError('')
    setMessage('')
    setIsSubmitting(true)
    try {
      await api.resendVerification({ username: username.trim() })
      setMessage(t('verify.sent'))
    } catch (err) {
      setError(err.message || t('verify.error'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-md-6">
          <div className="card bg-glass p-4">
            <h1 className="h3 mb-2"><T k="verify.title" /></h1>
            <p className="text-muted"><T k="verify.blurb" /></p>
            {error && <div className="alert alert-danger" role="alert">{error}</div>}
            {message && <div className="alert alert-success" role="alert">{message}</div>}
            <form onSubmit={handleSubmit}>
              <div className="mb-3">
                <label className="form-label" htmlFor="verify-username"><T k="verify.username" /></label>
                <input
                  id="verify-username"
                  className="form-control"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  autoComplete="username"
                  required
                />
              </div>
              <div className="mb-3">
                <label className="form-label" htmlFor="verify-code"><T k="verify.code" /></label>
                <input
                  id="verify-code"
                  className="form-control"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={8}
                  value={code}
                  onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 8))}
                  required
                />
              </div>
              <button type="submit" className="btn btn-primary me-2" disabled={isSubmitting}>
                <ScrambleText text={isSubmitting ? t('verify.submitting') : t('verify.submit')} />
              </button>
              <button type="button" className="btn btn-outline-secondary" onClick={handleResend} disabled={isSubmitting || !username.trim()}>
                <T k="verify.resend" />
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}
