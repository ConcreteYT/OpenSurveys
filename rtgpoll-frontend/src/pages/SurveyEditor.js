/**
 * Survey create/edit page (`/editor` or `/editor/:formId`).
 * Long-form glass-card editor: title + stacked questions, then save.
 * Create → POST /forms; edit → PUT /forms/{id}. After save, navigates to /surveys.
 */
import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { useT, T } from '../i18n'
import ScrambleText from '../animations/ScrambleText'

// Must match backend QuestionType.java / access.js
const QUESTION_TYPE = {
  SKIPPABLE_TEXT: 0,
  TEXT: 1,
  MULTIPLE_CHOICE: 2,
  RATING: 3,
}

const RATING_MAX_MIN = 5
const RATING_MAX_MAX = 10

function getTypeLabels(t) {
  return [
    { value: QUESTION_TYPE.SKIPPABLE_TEXT, label: t('editor.typeInfo') },
    { value: QUESTION_TYPE.TEXT, label: t('editor.typeText') },
    { value: QUESTION_TYPE.MULTIPLE_CHOICE, label: t('editor.typeMcq') },
    { value: QUESTION_TYPE.RATING, label: t('editor.typeRating') },
  ]
}

let nextClientKey = 1
function makeClientKey() {
  nextClientKey += 1
  return `q-${nextClientKey}`
}

function emptyQuestion() {
  return {
    clientKey: makeClientKey(),
    id: null,
    questionText: '',
    questionType: QUESTION_TYPE.TEXT,
    mcqOptions: ['', ''],
    selectCount: 1,
    ratingMax: 5,
  }
}

function parseMultipleChoice(questionOptions) {
  if (!questionOptions || typeof questionOptions !== 'string') {
    return { options: ['', ''], selectCount: 1 }
  }

  const parts = questionOptions
    .split(';')
    .map((part) => part.trim())
    .filter(Boolean)

  if (parts.length === 0) return { options: ['', ''], selectCount: 1 }

  let selectCount = 1
  const last = parts[parts.length - 1]
  const countMatch = last.match(/^(\d+)!$/)
  if (countMatch) {
    selectCount = Number(countMatch[1])
    parts.pop()
  }

  return {
    options: parts.length >= 2 ? parts : [...parts, ''],
    selectCount: Number.isInteger(selectCount) && selectCount >= 1 ? selectCount : 1,
  }
}

function parseRatingMax(questionOptions) {
  const max = Number(String(questionOptions || '').trim())
  if (!Number.isInteger(max) || max < RATING_MAX_MIN || max > RATING_MAX_MAX) return 5
  return max
}

function questionFromApi(question) {
  const type = Number(question.questionType)
  const base = {
    clientKey: makeClientKey(),
    id: question.id ?? null,
    questionText: question.questionText || '',
    questionType: Number.isInteger(type) ? type : QUESTION_TYPE.TEXT,
    mcqOptions: ['', ''],
    selectCount: 1,
    ratingMax: 5,
  }

  if (base.questionType === QUESTION_TYPE.MULTIPLE_CHOICE) {
    const mcq = parseMultipleChoice(question.questionOptions)
    base.mcqOptions = mcq.options
    base.selectCount = mcq.selectCount
  } else if (base.questionType === QUESTION_TYPE.RATING) {
    base.ratingMax = parseRatingMax(question.questionOptions)
  }

  return base
}

function buildQuestionOptions(draft) {
  if (draft.questionType === QUESTION_TYPE.MULTIPLE_CHOICE) {
    const options = draft.mcqOptions.map((opt) => opt.trim()).filter(Boolean)
    const selectCount = Math.min(
      Math.max(1, Number(draft.selectCount) || 1),
      Math.max(1, options.length)
    )
    if (selectCount === 1) return options.join(';')
    return `${options.join(';')};${selectCount}!`
  }
  if (draft.questionType === QUESTION_TYPE.RATING) {
    return String(draft.ratingMax)
  }
  return null
}

function validateDraft(name, questions, t) {
  if (!name.trim()) return t('editor.errName')
  if (questions.length === 0) return t('editor.errNoQuestions')

  for (let i = 0; i < questions.length; i += 1) {
    const q = questions[i]
    const label = t('editor.questionN', { n: i + 1 })
    if (!q.questionText.trim()) return t('editor.errQuestionText', { label })

    if (q.questionType === QUESTION_TYPE.MULTIPLE_CHOICE) {
      const options = q.mcqOptions.map((opt) => opt.trim()).filter(Boolean)
      if (options.length < 2) return t('editor.errMcqOptions', { label })
      const selectCount = Number(q.selectCount) || 1
      if (selectCount < 1 || selectCount > options.length) {
        return t('editor.errSelectCount', { label })
      }
    }

    if (q.questionType === QUESTION_TYPE.RATING) {
      const max = Number(q.ratingMax)
      if (!Number.isInteger(max) || max < RATING_MAX_MIN || max > RATING_MAX_MAX) {
        return t('editor.errRatingMax', { label, min: RATING_MAX_MIN, max: RATING_MAX_MAX })
      }
    }
  }

  return ''
}

function toApiPayload(name, questions, responsesPublic) {
  return {
    name: name.trim(),
    responsesPublic: Boolean(responsesPublic),
    questions: questions.map((q) => {
      const payload = {
        questionText: q.questionText.trim(),
        questionType: q.questionType,
        questionOptions: buildQuestionOptions(q),
      }
      if (q.id != null) payload.id = q.id
      return payload
    }),
  }
}

export default function SurveyEditor() {
  const t = useT()
  const navigate = useNavigate()
  const { formId } = useParams()
  const isEdit = Boolean(formId)

  const [name, setName] = useState('')
  const [responsesPublic, setResponsesPublic] = useState(true)
  const [questions, setQuestions] = useState([emptyQuestion()])
  const [loading, setLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [loadFailed, setLoadFailed] = useState(false)

  useEffect(() => {
    if (!isEdit) return undefined

    let cancelled = false

    async function loadForm() {
      setLoading(true)
      setError('')
      setLoadFailed(false)
      try {
        const form = await api.getForm(formId)
        if (cancelled) return

        const username =
          (typeof window !== 'undefined' &&
            (window.localStorage.getItem('username') || window.localStorage.getItem('userName'))) ||
          ''
        if (form.creatorUsername && username && form.creatorUsername !== username) {
          setError(t('editor.ownerOnly'))
          setLoadFailed(true)
          return
        }

        setName(form.name || '')
        setResponsesPublic(form.responsesPublic !== false)
        const loaded = Array.isArray(form.questions) ? form.questions.map(questionFromApi) : []
        setQuestions(loaded.length > 0 ? loaded : [emptyQuestion()])
      } catch (err) {
        if (!cancelled) {
          setError(err.message || t('editor.loadError'))
          setLoadFailed(true)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    loadForm()
    return () => {
      cancelled = true
    }
  }, [formId, isEdit, t])

  function updateQuestion(clientKey, patch) {
    setQuestions((prev) =>
      prev.map((q) => (q.clientKey === clientKey ? { ...q, ...patch } : q))
    )
  }

  function changeType(clientKey, nextType) {
    const type = Number(nextType)
    setQuestions((prev) =>
      prev.map((q) => {
        if (q.clientKey !== clientKey) return q
        return {
          ...q,
          questionType: type,
          mcqOptions: q.mcqOptions?.length >= 2 ? q.mcqOptions : ['', ''],
          selectCount: q.selectCount || 1,
          ratingMax: q.ratingMax || 5,
        }
      })
    )
  }

  function addQuestion() {
    setQuestions((prev) => [...prev, emptyQuestion()])
  }

  function removeQuestion(clientKey) {
    setQuestions((prev) => (prev.length <= 1 ? prev : prev.filter((q) => q.clientKey !== clientKey)))
  }

  function moveQuestion(clientKey, direction) {
    setQuestions((prev) => {
      const index = prev.findIndex((q) => q.clientKey === clientKey)
      if (index < 0) return prev
      const target = index + direction
      if (target < 0 || target >= prev.length) return prev
      const next = [...prev]
      const [item] = next.splice(index, 1)
      next.splice(target, 0, item)
      return next
    })
  }

  function updateMcqOption(clientKey, optionIndex, value) {
    setQuestions((prev) =>
      prev.map((q) => {
        if (q.clientKey !== clientKey) return q
        const mcqOptions = [...q.mcqOptions]
        mcqOptions[optionIndex] = value
        return { ...q, mcqOptions }
      })
    )
  }

  function addMcqOption(clientKey) {
    setQuestions((prev) =>
      prev.map((q) =>
        q.clientKey === clientKey ? { ...q, mcqOptions: [...q.mcqOptions, ''] } : q
      )
    )
  }

  function removeMcqOption(clientKey, optionIndex) {
    setQuestions((prev) =>
      prev.map((q) => {
        if (q.clientKey !== clientKey) return q
        if (q.mcqOptions.length <= 2) return q
        const mcqOptions = q.mcqOptions.filter((_, i) => i !== optionIndex)
        const selectCount = Math.min(q.selectCount, mcqOptions.length)
        return { ...q, mcqOptions, selectCount }
      })
    )
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const validationError = validateDraft(name, questions, t)
    if (validationError) {
      setError(validationError)
      return
    }

    setSaving(true)
    setError('')
    try {
      const payload = toApiPayload(name, questions, responsesPublic)
      if (isEdit) {
        await api.updateForm(formId, payload)
      } else {
        await api.createForm(payload)
      }
      navigate('/surveys')
    } catch (err) {
      setError(err.message || t('editor.saveError'))
    } finally {
      setSaving(false)
    }
  }

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

    .editor-question {
      border: 1px solid color-mix(in srgb, var(--accent) 14%, transparent);
      border-radius: 12px;
      padding: 1rem 1.1rem;
      background: color-mix(in srgb, var(--accent) 4%, transparent);
    }

    .editor-question + .editor-question {
      margin-top: 1rem;
    }
  `}</style>

          <div className="container px-2 px-sm-4 py-3 py-md-5 px-md-5 my-3 my-md-5">
            <div className="card bg-glass welcome-card p-3 p-md-5">
              <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
                <div>
                  <h1 className="display-6 fw-bold mb-2 welcome-heading">
                    <ScrambleText text={isEdit ? t('editor.editTitle') : t('editor.createTitle')} />
                  </h1>
                  <p className="lead text-muted mb-0">
                    <ScrambleText text={isEdit ? t('editor.editBlurb') : t('editor.createBlurb')} />
                  </p>
                </div>
                <Link to="/surveys" className="btn btn-outline-secondary">
                  <T k="editor.backSurveys" />
                </Link>
              </div>

              {loading && (
                <p className="text-muted mb-0" role="status">
                  {t('editor.loading')}
                </p>
              )}

              {!loading && error && (
                <div className="alert alert-danger" role="alert">
                  {error}
                </div>
              )}

              {!loading && loadFailed && (
                <Link to="/surveys" className="btn btn-outline-primary">
                  <T k="editor.backSurveys" />
                </Link>
              )}

              {!loading && !loadFailed && (
                <form onSubmit={handleSubmit}>
                  <div className="mb-4">
                    <label htmlFor="survey-name" className="form-label fw-semibold">
                      <T k="editor.surveyName" />
                    </label>
                    <input
                      id="survey-name"
                      type="text"
                      className="form-control form-control-lg"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder={t('editor.namePlaceholder')}
                      disabled={saving}
                      required
                    />
                  </div>

                  <div className="mb-4">
                    <div className="form-check form-switch">
                      <input
                        id="responses-public"
                        className="form-check-input"
                        type="checkbox"
                        role="switch"
                        checked={responsesPublic}
                        onChange={(e) => setResponsesPublic(e.target.checked)}
                        disabled={saving}
                      />
                      <label className="form-check-label fw-semibold" htmlFor="responses-public">
                        <T k="editor.responsesPublic" />
                      </label>
                    </div>
                    <p className="form-text text-muted mb-0 mt-1">
                      {responsesPublic
                        ? t('editor.responsesPublicHelp')
                        : t('editor.responsesPrivateHelp')}
                    </p>
                  </div>

                  <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                    <h2 className="h4 fw-bold mb-0 section-heading"><T k="editor.questions" /></h2>
                    <button
                      type="button"
                      className="btn btn-outline-primary"
                      onClick={addQuestion}
                      disabled={saving}
                    >
                      <T k="editor.addQuestion" />
                    </button>
                  </div>

                  {questions.map((question, index) => {
                    const optionCount = question.mcqOptions.map((o) => o.trim()).filter(Boolean).length
                    return (
                      <div className="editor-question" key={question.clientKey}>
                        <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                          <span className="fw-semibold"><T k="editor.questionN" vars={{ n: index + 1 }} /></span>
                          <div className="d-flex flex-wrap gap-2">
                            <button
                              type="button"
                              className="btn btn-sm btn-outline-secondary"
                              onClick={() => moveQuestion(question.clientKey, -1)}
                              disabled={saving || index === 0}
                              aria-label={t('editor.moveUp')}
                            >
                              <T k="editor.up" />
                            </button>
                            <button
                              type="button"
                              className="btn btn-sm btn-outline-secondary"
                              onClick={() => moveQuestion(question.clientKey, 1)}
                              disabled={saving || index === questions.length - 1}
                              aria-label={t('editor.moveDown')}
                            >
                              <T k="editor.down" />
                            </button>
                            <button
                              type="button"
                              className="btn btn-sm btn-outline-danger"
                              onClick={() => removeQuestion(question.clientKey)}
                              disabled={saving || questions.length <= 1}
                            >
                              <T k="editor.remove" />
                            </button>
                          </div>
                        </div>

                        <div className="mb-3">
                          <label className="form-label" htmlFor={`q-text-${question.clientKey}`}>
                            <T k="editor.questionText" />
                          </label>
                          <textarea
                            id={`q-text-${question.clientKey}`}
                            className="form-control"
                            rows={2}
                            value={question.questionText}
                            onChange={(e) =>
                              updateQuestion(question.clientKey, { questionText: e.target.value })
                            }
                            disabled={saving}
                            required
                          />
                        </div>

                        <div className="mb-3">
                          <label className="form-label" htmlFor={`q-type-${question.clientKey}`}>
                            <T k="editor.type" />
                          </label>
                          <select
                            id={`q-type-${question.clientKey}`}
                            className="form-select"
                            value={question.questionType}
                            onChange={(e) => changeType(question.clientKey, e.target.value)}
                            disabled={saving}
                          >
                            {getTypeLabels(t).map((type) => (
                              <option key={type.value} value={type.value}>
                                {type.label}
                              </option>
                            ))}
                          </select>
                        </div>

                        {question.questionType === QUESTION_TYPE.MULTIPLE_CHOICE && (
                          <div className="mb-1">
                            <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
                              <span className="form-label mb-0"><T k="editor.options" /></span>
                              <button
                                type="button"
                                className="btn btn-sm btn-outline-primary"
                                onClick={() => addMcqOption(question.clientKey)}
                                disabled={saving}
                              >
                                <T k="editor.addOption" />
                              </button>
                            </div>
                            {question.mcqOptions.map((option, optionIndex) => (
                              <div className="input-group mb-2" key={`${question.clientKey}-opt-${optionIndex}`}>
                                <input
                                  type="text"
                                  className="form-control"
                                  value={option}
                                  onChange={(e) =>
                                    updateMcqOption(question.clientKey, optionIndex, e.target.value)
                                  }
                                  placeholder={t('editor.optionN', { n: optionIndex + 1 })}
                                  disabled={saving}
                                />
                                <button
                                  type="button"
                                  className="btn btn-outline-secondary"
                                  onClick={() => removeMcqOption(question.clientKey, optionIndex)}
                                  disabled={saving || question.mcqOptions.length <= 2}
                                  aria-label={t('editor.removeOption')}
                                >
                                  ×
                                </button>
                              </div>
                            ))}
                            <div className="mt-2" style={{ maxWidth: 220 }}>
                              <label
                                className="form-label"
                                htmlFor={`q-select-${question.clientKey}`}
                              >
                                <T k="editor.selectCount" />
                              </label>
                              <input
                                id={`q-select-${question.clientKey}`}
                                type="number"
                                className="form-control"
                                min={1}
                                max={Math.max(1, optionCount || question.mcqOptions.length)}
                                value={question.selectCount}
                                onChange={(e) =>
                                  updateQuestion(question.clientKey, {
                                    selectCount: Number(e.target.value) || 1,
                                  })
                                }
                                disabled={saving}
                              />
                            </div>
                          </div>
                        )}

                        {question.questionType === QUESTION_TYPE.RATING && (
                          <div style={{ maxWidth: 220 }}>
                            <label className="form-label" htmlFor={`q-rating-${question.clientKey}`}>
                              <T k="editor.ratingMax" />
                            </label>
                            <input
                              id={`q-rating-${question.clientKey}`}
                              type="number"
                              className="form-control"
                              min={RATING_MAX_MIN}
                              max={RATING_MAX_MAX}
                              value={question.ratingMax}
                              onChange={(e) =>
                                updateQuestion(question.clientKey, {
                                  ratingMax: Number(e.target.value) || RATING_MAX_MIN,
                                })
                              }
                              disabled={saving}
                            />
                          </div>
                        )}
                      </div>
                    )
                  })}

                  <div className="d-flex flex-wrap gap-2 mt-4">
                    <button type="submit" className="btn btn-primary btn-lg px-4" disabled={saving}>
                      <ScrambleText
                        text={
                          saving
                            ? t('editor.saving')
                            : isEdit
                              ? t('editor.saveChanges')
                              : t('editor.create')
                        }
                      />
                    </button>
                    <button
                      type="button"
                      className="btn btn-outline-secondary btn-lg px-4"
                      onClick={() => navigate('/surveys')}
                      disabled={saving}
                    >
                      <T k="common.cancel" />
                    </button>
                  </div>
                </form>
              )}
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
