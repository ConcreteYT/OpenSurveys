/**
 * Account settings (`/settings`).
 * Profile (name/username), email, password, and account deletion.
 */
import React, { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, logout } from '../api/client'
import { useT, T } from '../i18n'
import ScrambleText from '../animations/ScrambleText'

function splitName(fullName) {
  const trimmed = (fullName || '').trim()
  if (!trimmed) return { firstName: '', lastName: '' }
  const space = trimmed.indexOf(' ')
  if (space < 0) return { firstName: trimmed, lastName: '' }
  return {
    firstName: trimmed.slice(0, space),
    lastName: trimmed.slice(space + 1).trim(),
  }
}

function applySession(profile) {
  if (profile?.username) {
    localStorage.setItem('username', profile.username)
  }
  if (profile?.role) {
    localStorage.setItem('role', profile.role)
  }
  if (profile?.token) {
    localStorage.setItem('token', profile.token)
  }
}

export default function SettingsPage() {
  const t = useT()
  const navigate = useNavigate()

  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [hasPassword, setHasPassword] = useState(true)
  const [currentEmail, setCurrentEmail] = useState('')
  const [accountUsername, setAccountUsername] = useState('')

  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [username, setUsername] = useState('')
  const [profileMessage, setProfileMessage] = useState('')
  const [profileError, setProfileError] = useState('')
  const [savingProfile, setSavingProfile] = useState(false)

  const [newEmail, setNewEmail] = useState('')
  const [emailPassword, setEmailPassword] = useState('')
  const [emailCode, setEmailCode] = useState('')
  const [emailMessage, setEmailMessage] = useState('')
  const [emailError, setEmailError] = useState('')
  const [savingEmail, setSavingEmail] = useState(false)
  const [sendingEmailCode, setSendingEmailCode] = useState(false)

  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [passwordCurrent, setPasswordCurrent] = useState('')
  const [passwordCode, setPasswordCode] = useState('')
  const [passwordMessage, setPasswordMessage] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [savingPassword, setSavingPassword] = useState(false)
  const [sendingPasswordCode, setSendingPasswordCode] = useState(false)

  const [deleteConfirm, setDeleteConfirm] = useState('')
  const [deleteError, setDeleteError] = useState('')
  const [deleting, setDeleting] = useState(false)

  const loadProfile = useCallback(async () => {
    const profile = await api.getMe()
    const parts = splitName(profile.name)
    setFirstName(parts.firstName)
    setLastName(parts.lastName)
    setUsername(profile.username || '')
    setAccountUsername(profile.username || '')
    setCurrentEmail(profile.email || '')
    setNewEmail(profile.email || '')
    setHasPassword(Boolean(profile.hasPassword))
  }, [])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        await loadProfile()
        if (!cancelled) setLoadError('')
      } catch (err) {
        if (!cancelled) setLoadError(err.message || t('settings.loadError'))
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [loadProfile, t])

  async function handleSaveProfile(event) {
    event.preventDefault()
    setSavingProfile(true)
    setProfileError('')
    setProfileMessage('')
    try {
      const updated = await api.updateProfile({
        name: `${firstName} ${lastName}`.trim(),
        username: username.trim(),
      })
      applySession(updated)
      setAccountUsername(updated.username || '')
      setProfileMessage(t('settings.profileSaved'))
    } catch (err) {
      setProfileError(err.message || t('settings.loadError'))
    } finally {
      setSavingProfile(false)
    }
  }

  async function handleSendEmailCode() {
    setSendingEmailCode(true)
    setEmailError('')
    setEmailMessage('')
    try {
      await api.sendAccountCode({ purpose: 'EMAIL_CHANGE' })
      setEmailMessage(t('settings.codeSent'))
    } catch (err) {
      setEmailError(err.message || t('settings.loadError'))
    } finally {
      setSendingEmailCode(false)
    }
  }

  async function handleSaveEmail(event) {
    event.preventDefault()
    setSavingEmail(true)
    setEmailError('')
    setEmailMessage('')
    try {
      const body = { email: newEmail.trim() }
      if (emailCode.trim()) {
        body.code = emailCode.trim()
      } else {
        body.currentPassword = emailPassword
      }
      const updated = await api.updateEmail(body)
      setCurrentEmail(updated.email || '')
      setNewEmail(updated.email || '')
      setEmailPassword('')
      setEmailCode('')
      setEmailMessage(t('settings.emailSaved'))
    } catch (err) {
      setEmailError(err.message || t('settings.loadError'))
    } finally {
      setSavingEmail(false)
    }
  }

  async function handleSendPasswordCode() {
    setSendingPasswordCode(true)
    setPasswordError('')
    setPasswordMessage('')
    try {
      await api.sendAccountCode({ purpose: 'PASSWORD_CHANGE' })
      setPasswordMessage(t('settings.codeSent'))
    } catch (err) {
      setPasswordError(err.message || t('settings.loadError'))
    } finally {
      setSendingPasswordCode(false)
    }
  }

  async function handleSavePassword(event) {
    event.preventDefault()
    if (newPassword !== confirmPassword) {
      setPasswordError(t('settings.passwordMismatch'))
      return
    }
    setSavingPassword(true)
    setPasswordError('')
    setPasswordMessage('')
    try {
      const body = { newPassword }
      if (passwordCode.trim() || !hasPassword) {
        body.code = passwordCode.trim()
      } else {
        body.currentPassword = passwordCurrent
      }
      const updated = await api.updatePassword(body)
      setHasPassword(Boolean(updated.hasPassword))
      setNewPassword('')
      setConfirmPassword('')
      setPasswordCurrent('')
      setPasswordCode('')
      setPasswordMessage(t('settings.passwordSaved'))
    } catch (err) {
      setPasswordError(err.message || t('settings.loadError'))
    } finally {
      setSavingPassword(false)
    }
  }

  async function handleDeleteAccount(event) {
    event.preventDefault()
    if (deleteConfirm.trim() !== accountUsername) {
      setDeleteError(t('settings.deleteMismatch'))
      return
    }
    setDeleting(true)
    setDeleteError('')
    try {
      await api.deleteAccount({ confirmUsername: deleteConfirm.trim() })
      logout()
      navigate('/home', { replace: true })
    } catch (err) {
      setDeleteError(err.message || t('settings.loadError'))
      setDeleting(false)
    }
  }

  const busy =
    savingProfile ||
    savingEmail ||
    savingPassword ||
    deleting ||
    sendingEmailCode ||
    sendingPasswordCode

  return (
    <div className="container py-4" style={{ maxWidth: 720 }}>
      <div className="mb-4">
        <h1 className="h3 mb-1">
          <T k="settings.title" />
        </h1>
        <p className="text-muted mb-0">
          <T k="settings.blurb" />
        </p>
      </div>

      {loading && <p className="text-muted mb-0">{t('settings.loading')}</p>}

      {!loading && loadError && (
        <div className="alert alert-danger" role="alert">
          {loadError}
        </div>
      )}

      {!loading && !loadError && (
        <>
          <section className="mb-5">
            <h2 className="h5 mb-1">
              <T k="settings.profile" />
            </h2>
            <p className="text-muted small mb-3">
              <T k="settings.profileBlurb" />
            </p>
            {profileError && (
              <div className="alert alert-danger" role="alert">
                {profileError}
              </div>
            )}
            {profileMessage && (
              <div className="alert alert-success" role="alert">
                {profileMessage}
              </div>
            )}
            <form onSubmit={handleSaveProfile}>
              <div className="row g-3 mb-3">
                <div className="col-md-6">
                  <label className="form-label" htmlFor="settings-first-name">
                    <T k="settings.firstName" />
                  </label>
                  <input
                    id="settings-first-name"
                    className="form-control"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    disabled={busy}
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label" htmlFor="settings-last-name">
                    <T k="settings.lastName" />
                  </label>
                  <input
                    id="settings-last-name"
                    className="form-control"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    disabled={busy}
                  />
                </div>
              </div>
              <div className="mb-3">
                <label className="form-label" htmlFor="settings-username">
                  <T k="settings.username" />
                </label>
                <input
                  id="settings-username"
                  className="form-control"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  disabled={busy}
                />
              </div>
              <button type="submit" className="btn btn-primary" disabled={busy}>
                <ScrambleText
                  text={savingProfile ? t('settings.saving') : t('settings.saveProfile')}
                />
              </button>
            </form>
          </section>

          <section className="mb-5">
            <h2 className="h5 mb-1">
              <T k="settings.email" />
            </h2>
            <p className="text-muted small mb-3">
              <T k="settings.emailBlurb" />
            </p>
            {emailError && (
              <div className="alert alert-danger" role="alert">
                {emailError}
              </div>
            )}
            {emailMessage && (
              <div className="alert alert-success" role="alert">
                {emailMessage}
              </div>
            )}
            <form onSubmit={handleSaveEmail}>
              <div className="mb-3">
                <label className="form-label" htmlFor="settings-current-email">
                  <T k="settings.currentEmail" />
                </label>
                <input
                  id="settings-current-email"
                  className="form-control"
                  value={currentEmail}
                  disabled
                  readOnly
                />
              </div>
              <div className="mb-3">
                <label className="form-label" htmlFor="settings-new-email">
                  <T k="settings.newEmail" />
                </label>
                <input
                  id="settings-new-email"
                  type="email"
                  className="form-control"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  required
                  disabled={busy}
                />
              </div>
              {hasPassword && (
                <div className="mb-3">
                  <label className="form-label" htmlFor="settings-email-password">
                    <T k="settings.currentPassword" />
                  </label>
                  <input
                    id="settings-email-password"
                    type="password"
                    className="form-control"
                    value={emailPassword}
                    onChange={(e) => setEmailPassword(e.target.value)}
                    disabled={busy || Boolean(emailCode.trim())}
                    autoComplete="current-password"
                  />
                </div>
              )}
              <p className="text-muted small mb-2">
                <T k="settings.orVerifyWith" />
              </p>
              <div className="mb-3">
                <label className="form-label" htmlFor="settings-email-code">
                  <T k="settings.verificationCode" />
                </label>
                <div className="input-group">
                  <input
                    id="settings-email-code"
                    className="form-control"
                    value={emailCode}
                    onChange={(e) => setEmailCode(e.target.value)}
                    disabled={busy || Boolean(emailPassword.trim())}
                    inputMode="numeric"
                    autoComplete="one-time-code"
                  />
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={handleSendEmailCode}
                    disabled={busy}
                  >
                    <ScrambleText
                      text={
                        sendingEmailCode ? t('settings.sendingCode') : t('settings.sendCode')
                      }
                    />
                  </button>
                </div>
              </div>
              <button type="submit" className="btn btn-primary" disabled={busy}>
                <ScrambleText
                  text={savingEmail ? t('settings.saving') : t('settings.saveEmail')}
                />
              </button>
            </form>
          </section>

          <section className="mb-5">
            <h2 className="h5 mb-1">
              <T k="settings.password" />
            </h2>
            <p className="text-muted small mb-3">
              <T k={hasPassword ? 'settings.passwordBlurb' : 'settings.passwordBlurbNoPassword'} />
            </p>
            {passwordError && (
              <div className="alert alert-danger" role="alert">
                {passwordError}
              </div>
            )}
            {passwordMessage && (
              <div className="alert alert-success" role="alert">
                {passwordMessage}
              </div>
            )}
            <form onSubmit={handleSavePassword}>
              <div className="mb-3">
                <label className="form-label" htmlFor="settings-new-password">
                  <T k="settings.newPassword" />
                </label>
                <input
                  id="settings-new-password"
                  type="password"
                  className="form-control"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  minLength={6}
                  disabled={busy}
                  autoComplete="new-password"
                />
              </div>
              <div className="mb-3">
                <label className="form-label" htmlFor="settings-confirm-password">
                  <T k="settings.confirmPassword" />
                </label>
                <input
                  id="settings-confirm-password"
                  type="password"
                  className="form-control"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  minLength={6}
                  disabled={busy}
                  autoComplete="new-password"
                />
              </div>
              {hasPassword && (
                <div className="mb-3">
                  <label className="form-label" htmlFor="settings-password-current">
                    <T k="settings.currentPassword" />
                  </label>
                  <input
                    id="settings-password-current"
                    type="password"
                    className="form-control"
                    value={passwordCurrent}
                    onChange={(e) => setPasswordCurrent(e.target.value)}
                    disabled={busy || Boolean(passwordCode.trim())}
                    autoComplete="current-password"
                  />
                </div>
              )}
              <p className="text-muted small mb-2">
                <T k="settings.orVerifyWith" />
              </p>
              <div className="mb-3">
                <label className="form-label" htmlFor="settings-password-code">
                  <T k="settings.verificationCode" />
                </label>
                <div className="input-group">
                  <input
                    id="settings-password-code"
                    className="form-control"
                    value={passwordCode}
                    onChange={(e) => setPasswordCode(e.target.value)}
                    disabled={busy || Boolean(passwordCurrent.trim())}
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    required={!hasPassword}
                  />
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={handleSendPasswordCode}
                    disabled={busy}
                  >
                    <ScrambleText
                      text={
                        sendingPasswordCode
                          ? t('settings.sendingCode')
                          : t('settings.sendCode')
                      }
                    />
                  </button>
                </div>
              </div>
              <button type="submit" className="btn btn-primary" disabled={busy}>
                <ScrambleText
                  text={savingPassword ? t('settings.saving') : t('settings.savePassword')}
                />
              </button>
            </form>
          </section>

          <section className="mb-4 border border-danger-subtle rounded p-3">
            <h2 className="h5 text-danger mb-1">
              <T k="settings.danger" />
            </h2>
            <p className="text-muted small mb-3">
              <T k="settings.dangerBlurb" />
            </p>
            {deleteError && (
              <div className="alert alert-danger" role="alert">
                {deleteError}
              </div>
            )}
            <form onSubmit={handleDeleteAccount}>
              <div className="mb-3">
                <label className="form-label" htmlFor="settings-delete-confirm">
                  {t('settings.deleteConfirmLabel', { username: accountUsername })}
                </label>
                <input
                  id="settings-delete-confirm"
                  className="form-control"
                  value={deleteConfirm}
                  onChange={(e) => setDeleteConfirm(e.target.value)}
                  disabled={busy}
                  autoComplete="off"
                />
              </div>
              <button
                type="submit"
                className="btn btn-danger"
                disabled={busy || deleteConfirm.trim() !== accountUsername}
              >
                <ScrambleText
                  text={deleting ? t('settings.deleting') : t('settings.deleteAccount')}
                />
              </button>
            </form>
          </section>
        </>
      )}
    </div>
  )
}
