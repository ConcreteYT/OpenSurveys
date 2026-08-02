/**
 * Admin user directory (`/admin`).
 * Lists accounts and lets admins edit details, toggle role, or delete a user.
 */
import React, { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, isAdmin, logout } from '../api/client'
import { useT, T } from '../i18n'
import ScrambleText from '../animations/ScrambleText'

export default function AdminPage() {
  const t = useT()
  const navigate = useNavigate()
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [actionMessage, setActionMessage] = useState('')
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)
  const [deletingId, setDeletingId] = useState(null)

  const currentUsername = typeof window !== 'undefined' ? localStorage.getItem('username') : null

  const loadUsers = useCallback(async () => {
    const data = await api.listUsers()
    setUsers(Array.isArray(data) ? data : [])
  }, [])

  useEffect(() => {
    if (!isAdmin()) {
      navigate('/login-home', { replace: true })
      return
    }

    let cancelled = false

    ;(async () => {
      try {
        await loadUsers()
        if (!cancelled) setError('')
      } catch (err) {
        if (!cancelled) setError(err.message || t('admin.loadError'))
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [navigate, loadUsers, t])

  function openEdit(user) {
    setActionError('')
    setActionMessage('')
    setEditing({
      id: user.id,
      name: user.name || '',
      username: user.username || '',
      email: user.email || '',
      role: user.role === 'ADMIN' ? 'ADMIN' : 'USER',
    })
  }

  function closeEdit() {
    if (saving) return
    setEditing(null)
  }

  async function handleSave(event) {
    event.preventDefault()
    if (!editing) return

    setSaving(true)
    setActionError('')
    setActionMessage('')
    try {
      const updated = await api.updateUser(editing.id, {
        name: editing.name.trim(),
        username: editing.username.trim(),
        email: editing.email.trim(),
        role: editing.role,
      })

      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)))
      setEditing(null)
      setActionMessage(t('admin.updated', { username: updated.username }))

      // Keep local session in sync if the signed-in admin edited themselves.
      if (currentUsername && (currentUsername === editing.username || currentUsername === updated.username)) {
        localStorage.setItem('username', updated.username)
        localStorage.setItem('role', updated.role || 'USER')
        if (updated.role !== 'ADMIN') {
          logout()
          navigate('/home', { replace: true })
        }
      }
    } catch (err) {
      setActionError(err.message || t('admin.updateError'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(user) {
    if (user.username === currentUsername) {
      setActionError(t('admin.deleteOwnError'))
      return
    }

    const confirmed = window.confirm(t('admin.deleteConfirm', { username: user.username }))
    if (!confirmed) return

    setDeletingId(user.id)
    setActionError('')
    setActionMessage('')
    try {
      await api.deleteUser(user.id)
      setUsers((prev) => prev.filter((u) => u.id !== user.id))
      if (editing?.id === user.id) setEditing(null)
      setActionMessage(t('admin.deleted', { username: user.username }))
    } catch (err) {
      setActionError(err.message || t('admin.deleteError'))
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className="container py-4">
      <div className="d-flex flex-wrap align-items-end justify-content-between gap-2 mb-4">
        <div>
          <h1 className="h3 mb-1"><T k="admin.title" /></h1>
          <p className="text-muted mb-0"><T k="admin.blurb" /></p>
        </div>
        {!loading && !error && (
          <span className="badge text-bg-secondary">
            {t('admin.usersCount', { count: users.length })}
          </span>
        )}
      </div>

      {loading && <p className="text-muted mb-0">{t('admin.loading')}</p>}

      {!loading && error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      {!loading && !error && actionError && (
        <div className="alert alert-danger" role="alert">
          {actionError}
        </div>
      )}

      {!loading && !error && actionMessage && (
        <div className="alert alert-success" role="alert">
          {actionMessage}
        </div>
      )}

      {!loading && !error && (
        <div className="table-responsive">
          <table className="table table-hover align-middle">
            <thead>
              <tr>
                <th scope="col"><T k="admin.colId" /></th>
                <th scope="col"><T k="admin.colName" /></th>
                <th scope="col"><T k="admin.colUsername" /></th>
                <th scope="col"><T k="admin.colEmail" /></th>
                <th scope="col"><T k="admin.colRole" /></th>
                <th scope="col" className="text-end">
                  <T k="admin.colActions" />
                </th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-muted">
                    <T k="admin.empty" />
                  </td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>{user.name || '—'}</td>
                    <td>
                      {user.username}
                      {user.username === currentUsername ? (
                        <span className="text-muted ms-1"><T k="admin.you" /></span>
                      ) : null}
                    </td>
                    <td>{user.email || '—'}</td>
                    <td>
                      <span
                        className={`badge ${
                          user.role === 'ADMIN' ? 'text-bg-warning' : 'text-bg-secondary'
                        }`}
                      >
                        {user.role || 'USER'}
                      </span>
                    </td>
                    <td className="text-end text-nowrap">
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-primary me-2"
                        onClick={() => openEdit(user)}
                        disabled={saving || deletingId != null}
                      >
                        <T k="admin.edit" />
                      </button>
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-danger"
                        onClick={() => handleDelete(user)}
                        disabled={
                          saving ||
                          deletingId != null ||
                          user.username === currentUsername
                        }
                        title={
                          user.username === currentUsername
                            ? t('admin.deleteOwn')
                            : t('admin.deleteTitle')
                        }
                      >
                        <ScrambleText text={deletingId === user.id ? t('admin.deleting') : t('admin.delete')} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {editing && (
        <div
          className="modal d-block"
          tabIndex={-1}
          role="dialog"
          aria-modal="true"
          aria-labelledby="editUserTitle"
          style={{ backgroundColor: 'rgba(0,0,0,0.45)' }}
          onClick={closeEdit}
        >
          <div
            className="modal-dialog modal-dialog-centered"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-content">
              <form onSubmit={handleSave}>
                <div className="modal-header">
                  <h2 className="modal-title h5" id="editUserTitle">
                    <T k="admin.editTitle" />
                  </h2>
                  <button
                    type="button"
                    className="btn-close"
                    aria-label={t('admin.close')}
                    onClick={closeEdit}
                    disabled={saving}
                  />
                </div>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label" htmlFor="edit-name">
                      <T k="common.name" />
                    </label>
                    <input
                      id="edit-name"
                      className="form-control"
                      value={editing.name}
                      onChange={(e) => setEditing({ ...editing, name: e.target.value })}
                      disabled={saving}
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label" htmlFor="edit-username">
                      <T k="common.username" />
                    </label>
                    <input
                      id="edit-username"
                      className="form-control"
                      value={editing.username}
                      onChange={(e) => setEditing({ ...editing, username: e.target.value })}
                      required
                      disabled={saving}
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label" htmlFor="edit-email">
                      <T k="common.email" />
                    </label>
                    <input
                      id="edit-email"
                      type="email"
                      className="form-control"
                      value={editing.email}
                      onChange={(e) => setEditing({ ...editing, email: e.target.value })}
                      disabled={saving}
                    />
                  </div>
                  <div className="mb-0">
                    <label className="form-label" htmlFor="edit-role">
                      <T k="admin.role" />
                    </label>
                    <select
                      id="edit-role"
                      className="form-select"
                      value={editing.role}
                      onChange={(e) => setEditing({ ...editing, role: e.target.value })}
                      disabled={saving}
                    >
                      <option value="USER">USER</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={closeEdit}
                    disabled={saving}
                  >
                    <T k="admin.cancel" />
                  </button>
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    <ScrambleText text={saving ? t('admin.saving') : t('admin.save')} />
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
