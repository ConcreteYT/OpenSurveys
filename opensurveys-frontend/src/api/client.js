// Thin wrapper around fetch for talking to the Spring Boot backend.
// The session JWT is an httpOnly cookie. localStorage only keeps a UI hint.
const API_BASE_URL = process.env.REACT_APP_API_URL || '';

const AUTH_KEYS = ['token', 'username', 'userName', 'role', 'session'];

export function clearAuth() {
  AUTH_KEYS.forEach((key) => localStorage.removeItem(key));
}

export function markSession(role, username) {
  localStorage.setItem('session', '1');
  if (username) {
    localStorage.setItem('username', username);
    localStorage.setItem('userName', username);
  }
  if (role) {
    localStorage.setItem('role', role);
  }
}

export function isAuthenticated() {
  return localStorage.getItem('session') === '1';
}

export function isAdmin() {
  return isAuthenticated() && localStorage.getItem('role') === 'ADMIN';
}

export function logout() {
  clearAuth();
  fetch(`${API_BASE_URL}/auth/logout`, { method: 'POST', credentials: 'include' }).catch(() => {});
}

export function rememberSurveyCode(formId, code) {
  sessionStorage.setItem(`survey-code-${formId}`, code);
}

export function surveyCodeFor(formId) {
  return sessionStorage.getItem(`survey-code-${formId}`);
}

export function mediaUrl(formId, fileName) {
  return `${API_BASE_URL}/forms/${formId}/media/${encodeURIComponent(fileName)}`;
}

async function readError(response, auth) {
  if (response.status === 401 && auth) {
    clearAuth();
  }
  const errorBody = await response.json().catch(() => null);
  const error = new Error(errorBody?.error || `Request failed with status ${response.status}`);
  error.status = response.status;
  error.body = errorBody;
  return error;
}

async function requestMultipart(path, formData, { auth = false } = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    credentials: 'include',
    body: formData,
  });

  if (!response.ok) {
    throw await readError(response, auth);
  }
  return response.json();
}

async function requestBlob(path, { auth = true } = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'GET',
    credentials: 'include',
  });

  if (!response.ok) {
    throw await readError(response, auth);
  }
  return response.blob();
}

async function request(path, { method = 'GET', body, auth = false } = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    throw await readError(response, auth);
  }

  if (response.status === 204) return null;
  return response.json();
}

export const api = {
  signup: (data) => request('/auth/signup', { method: 'POST', body: data }),
  login: (data) => request('/auth/login', { method: 'POST', body: data }),
  googleLogin: (data) => request('/auth/google', { method: 'POST', body: data }),
  verifyEmail: (data) => request('/auth/verify', { method: 'POST', body: data }),
  resendVerification: (data) => request('/auth/resend', { method: 'POST', body: data }),
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
  getForm: (id) => request(`/forms/${id}`, { auth: true }),
  getFormByAccessCode: (code) => request(`/forms/by-code/${encodeURIComponent(code)}`),
  getFormResponses: (id) => request(`/forms/${id}/responses`, { auth: isAuthenticated() }),
  submitAnswers: (id, data) => request(`/forms/${id}/answers`, { method: 'POST', body: data }),
  stageQuestionFiles: (formId, stagingId, questionId, files) => {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    return requestMultipart(
      `/forms/${formId}/staging/${stagingId}/questions/${questionId}/files`,
      formData,
    );
  },
  commitSubmission: (formId, stagingId, data) =>
    request(`/forms/${formId}/staging/${stagingId}/commit`, { method: 'POST', body: data }),
  deleteForm: (id) => request(`/forms/${id}`, { method: 'DELETE', auth: true }),
  exportForm: (id) => requestBlob(`/forms/${id}/export`, { auth: true }),
};
