// Thin wrapper around fetch for talking to the Spring Boot backend.
//
// In development, requests to relative paths (e.g. "/auth/login") are forwarded to
// http://localhost:8080 by the "proxy" field in package.json, so no CORS setup is
// needed on this side during `npm start`. In production, set REACT_APP_API_URL to the
// deployed backend's URL (proxy only works with the CRA dev server, not the built app).
const API_BASE_URL = process.env.REACT_APP_API_URL || '';

const AUTH_KEYS = ['token', 'username', 'userName', 'role'];

export function clearAuth() {
  AUTH_KEYS.forEach((key) => localStorage.removeItem(key));
}

function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return typeof payload.exp !== 'number' || payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}

/** True when a non-expired JWT is present; clears storage if the token is missing/invalid. */
export function isAuthenticated() {
  const token = localStorage.getItem('token');
  if (!token || isTokenExpired(token)) {
    clearAuth();
    return false;
  }
  return true;
}

/** UI gate for admin pages; backend still enforces ROLE_ADMIN on GET /users. */
export function isAdmin() {
  return isAuthenticated() && localStorage.getItem('role') === 'ADMIN';
}

export function logout() {
  clearAuth();
}

function authHeader() {
  const token = localStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request(path, { method = 'GET', body, auth = false } = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(auth ? authHeader() : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    // Only clear session on 401 when we sent a bearer token — login/signup 401s
    // mean bad credentials, not an expired session.
    if (response.status === 401 && auth) {
      clearAuth();
    }
    const errorBody = await response.json().catch(() => null);
    throw new Error(errorBody?.error || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) return null;
  return response.json();
}

export const api = {
  signup: (data) => request('/auth/signup', { method: 'POST', body: data }),
  login: (data) => request('/auth/login', { method: 'POST', body: data }),
  googleLogin: (data) => request('/auth/google', { method: 'POST', body: data }),
  getMe: () => request('/account/me', { auth: true }),
  updateProfile: (data) => request('/account/profile', { method: 'PUT', body: data, auth: true }),
  updateEmail: (data) => request('/account/email', { method: 'PUT', body: data, auth: true }),
  updatePassword: (data) => request('/account/password', { method: 'PUT', body: data, auth: true }),
  sendAccountCode: (data) => request('/account/send-code', { method: 'POST', body: data, auth: true }),
  deleteAccount: (data) => request('/account', { method: 'DELETE', body: data, auth: true }),
  listUsers: () => request('/users', { auth: true }),
  updateUser: (id, data) => request(`/users/${id}`, { method: 'PUT', body: data, auth: true }),
  deleteUser: (id) => request(`/users/${id}`, { method: 'DELETE', auth: true }),
  createForm: (data) => request('/forms', { method: 'POST', body: data, auth: true }),
  updateForm: (id, data) => request(`/forms/${id}`, { method: 'PUT', body: data, auth: true }),
  listMyForms: () => request('/forms', { auth: true }),
  getForm: (id) => request(`/forms/${id}`),
  // Send token when present so owners can load private responses; endpoint stays public for public forms.
  getFormResponses: (id) => request(`/forms/${id}/responses`, { auth: isAuthenticated() }),
  submitAnswers: (id, data) => request(`/forms/${id}/answers`, { method: 'POST', body: data }),
};
