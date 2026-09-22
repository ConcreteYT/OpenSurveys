/**
 * Survey fill-out and results page (`/access?formId=...`).
 *
 * Modes:
 * - Default: step through questions (text / MCQ / rating / skippable), submit answers,
 *   and mark completion in localStorage so the same browser cannot resubmit.
 * - `?view=responses`: show aggregated answer charts/percentages for the form.
 *
 * Question-type encoding must stay in sync with backend QuestionType.java.
 */
import React, { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { animate } from 'animejs'
import { api, mediaUrl } from '../api/client'
import { useT } from '../i18n'

// Must match backend QuestionType (QUESTION.questiontype):
//   0 = SKIPPABLE_TEXT (display-only; no answer field)
//   1 = TEXT (user must type their own answer)
//   2 = MULTIPLE_CHOICE (semicolon options; optional trailing N! = how many to pick, default 1)
//   3 = RATING (stars 1..max; questionOptions is max stars, whole number 5-10)
const QUESTION_TYPE = {
  SKIPPABLE_TEXT: 0,
  TEXT: 1,
  MULTIPLE_CHOICE: 2,
  RATING: 3,
  IMAGE_UPLOAD: 4,
}

const IMAGE_UPLOAD_MAX_BYTES = 100 * 1024 * 1024
const ALLOWED_UPLOAD_MIME = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif'])

const RATING_MAX_MIN = 5
const RATING_MAX_MAX = 10
const IMAGE_UPLOAD_MAX_FILES = 10

function parseImageUploadMaxFiles(questionOptions) {
  const max = Number(String(questionOptions || '').trim())
  if (!Number.isInteger(max) || max < 1 || max > IMAGE_UPLOAD_MAX_FILES) return 1
  return max
}

function splitUploadFileNames(answer) {
  if (!answer || typeof answer !== 'string') return []
  return answer.split(';').map((part) => part.trim()).filter(Boolean)
}

function formHasUploadQuestions(questionList) {
  return questionList.some((q) => q.questionType === QUESTION_TYPE.IMAGE_UPLOAD)
}

function validateUploadFiles(files, maxFiles, t) {
  if (!files || files.length === 0) {
    return t('access.uploadRequired')
  }
  if (files.length > maxFiles) {
    return t('access.uploadTooMany', { max: maxFiles })
  }
  let total = 0
  for (const file of files) {
    total += file.size
    const type = (file.type || '').toLowerCase()
    if (!ALLOWED_UPLOAD_MIME.has(type)) {
      return t('access.uploadTypeInvalid')
    }
  }
  if (total > IMAGE_UPLOAD_MAX_BYTES) {
    return t('access.uploadTooLarge')
  }
  return ''
}

function MediaThumbnail({ formId, fileName, authenticated }) {
  const [src, setSrc] = React.useState(authenticated ? null : mediaUrl(formId, fileName))

  React.useEffect(() => {
    if (!authenticated) {
      setSrc(mediaUrl(formId, fileName))
      return undefined
    }
    let objectUrl
    let cancelled = false
    const token = localStorage.getItem('token')
    fetch(mediaUrl(formId, fileName), {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
      .then((res) => {
        if (!res.ok) throw new Error('Failed to load image')
        return res.blob()
      })
      .then((blob) => {
        if (cancelled) return
        objectUrl = URL.createObjectURL(blob)
        setSrc(objectUrl)
      })
      .catch(() => {
        if (!cancelled) setSrc(null)
      })
    return () => {
      cancelled = true
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [formId, fileName, authenticated])

  if (!src) {
    return <div className="results-upload-thumb results-upload-thumb--loading" />
  }
  return (
    <img
      src={src}
      alt=""
      className="results-upload-thumb"
      loading="lazy"
    />
  )
}

function completedKey(formId) {
  return `survey-completed-${formId}`
}

// Parses MCQ questionOptions: "A;B;C" or "A;B;C;2!".
// Returns { options, selectCount } or null if invalid.
function parseMultipleChoice(questionOptions) {
  if (!questionOptions || typeof questionOptions !== 'string') return null

  const parts = questionOptions
    .split(';')
    .map((part) => part.trim())
    .filter(Boolean)

  if (parts.length === 0) return null

  let selectCount = 1
  const last = parts[parts.length - 1]
  const countMatch = last.match(/^(\d+)!$/)
  if (countMatch) {
    selectCount = Number(countMatch[1])
    parts.pop()
  }

  if (parts.length === 0) return null
  if (!Number.isInteger(selectCount) || selectCount < 1 || selectCount > parts.length) {
    return null
  }

  return { options: parts, selectCount }
}

// questionOptions for RATING is the max star count (5-10), e.g. "5" or "10".
function parseRatingMax(questionOptions) {
  if (!questionOptions || typeof questionOptions !== 'string') return null
  const max = Number(questionOptions.trim())
  if (!Number.isInteger(max) || max < RATING_MAX_MIN || max > RATING_MAX_MAX) return null
  return max
}

function normalizeAnswerList(values) {
  return Array.isArray(values)
    ? values.map((value) => String(value || '').trim()).filter(Boolean)
    : []
}

function buildMcqResults(questionOptions, answers) {
  const mcq = parseMultipleChoice(questionOptions)
  if (!mcq) return null

  const normalizedAnswers = normalizeAnswerList(answers)
  const totalResponses = normalizedAnswers.length
  const counts = new Map(mcq.options.map((option) => [option, 0]))

  normalizedAnswers.forEach((value) => {
    value
      .split(';')
      .map((part) => part.trim())
      .filter(Boolean)
      .forEach((selected) => {
        if (counts.has(selected)) {
          counts.set(selected, counts.get(selected) + 1)
        }
      })
  })

  return {
    totalResponses,
    items: mcq.options.map((option) => {
      const count = counts.get(option) || 0
      return {
        option,
        count,
        percent: totalResponses === 0 ? 0 : Math.round((count / totalResponses) * 100),
      }
    }),
  }
}

function buildRatingResults(questionOptions, answers) {
  const max = parseRatingMax(questionOptions)
  if (max == null) return null

  const normalizedAnswers = normalizeAnswerList(answers)
  const totalResponses = normalizedAnswers.length
  const counts = new Map(Array.from({ length: max }, (_, i) => [String(i + 1), 0]))

  normalizedAnswers.forEach((value) => {
    if (counts.has(value)) {
      counts.set(value, counts.get(value) + 1)
    }
  })

  return {
    totalResponses,
    items: Array.from({ length: max }, (_, i) => {
      const value = String(i + 1)
      const count = counts.get(value) || 0
      return {
        value: Number(value),
        count,
        percent: totalResponses === 0 ? 0 : Math.round((count / totalResponses) * 100),
      }
    }),
  }
}

// Survey fill-out page: loads a form by id, walks through every question one by one,
// supports display-only text / typed text / multiple choice / star rating, submits all
// answers at the end, then shows a thank-you screen so the survey cannot be answered
// again from this browser.
export default function Access() {
  const t = useT()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const formId = searchParams.get('formId')
  const shouldOpenResponses = searchParams.get('view') === 'responses'

  const [form, setForm] = useState(null)
  const [questions, setQuestions] = useState([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [answers, setAnswers] = useState({})
  const [answer, setAnswer] = useState('')
  const [selectedChoices, setSelectedChoices] = useState([])
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isFinalizing, setIsFinalizing] = useState(false)
  const [isCompleted, setIsCompleted] = useState(false)
  const [responsesData, setResponsesData] = useState(null)
  const [responsesIndex, setResponsesIndex] = useState(0)
  const [isLoadingResponses, setIsLoadingResponses] = useState(false)
  const [isViewingResponses, setIsViewingResponses] = useState(false)
  const [uploadFilesByQuestion, setUploadFilesByQuestion] = useState({})
  const [pendingUploadFiles, setPendingUploadFiles] = useState([])
  const [uploadProgress, setUploadProgress] = useState(null)
  const questionPanelRef = useRef(null)
  const progressFillRef = useRef(null)
  const autoOpenedResponsesRef = useRef(false)

  useEffect(() => {
    // Always reset when formId changes so a completed survey does not
    // block opening a different one.
    setForm(null)
    setQuestions([])
    setCurrentIndex(0)
    setAnswers({})
    setAnswer('')
    setSelectedChoices([])
    setError('')
    setIsSubmitting(false)
    setIsFinalizing(false)
    setIsCompleted(false)
    setResponsesData(null)
    setResponsesIndex(0)
    setIsLoadingResponses(false)
    setIsViewingResponses(false)
    setUploadFilesByQuestion({})
    setPendingUploadFiles([])
    setUploadProgress(null)
    autoOpenedResponsesRef.current = false

    if (!formId) {
      setError(t('access.noFormId'))
      setIsLoading(false)
      return
    }

    const hasCompletedThisForm = typeof window !== 'undefined'
      && window.localStorage.getItem(completedKey(formId))

    let cancelled = false

    const loadForm = async () => {
      setError('')
      setIsLoading(true)
      try {
        const data = await api.getForm(formId)
        if (cancelled) return

        const formQuestions = data.questions || []
        if (formQuestions.length === 0) {
          setError(t('access.noQuestions'))
          setForm(data)
          setIsCompleted(false)
          return
        }

        setForm(data)
        setQuestions(formQuestions)
        setCurrentIndex(0)
        setAnswers({})
        setAnswer('')
        setSelectedChoices([])
        setIsCompleted(Boolean(hasCompletedThisForm))
      } catch (err) {
        if (!cancelled) {
          setError(err.message || t('access.loadError'))
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    loadForm()
    return () => { cancelled = true }
  }, [formId, t])

  const question = questions[currentIndex] || null
  const responseQuestions = responsesData?.questions || []
  const responseQuestion = responseQuestions[responsesIndex] || null
  const isShowingResponses = isViewingResponses && !!responseQuestion
  const questionType = question?.questionType ?? QUESTION_TYPE.TEXT
  const totalQuestions = questions.length
  const questionNumber = question ? currentIndex + 1 : 0
  const isLastQuestion = currentIndex === totalQuestions - 1
  const responseQuestionNumber = responseQuestion ? responsesIndex + 1 : 0
  const responseProgressPercent = responseQuestions.length === 0
    ? 0
    : Math.round((responseQuestionNumber / responseQuestions.length) * 100)
  const progressPercent = isShowingResponses
    ? responseProgressPercent
    : totalQuestions === 0
    ? 0
    : isCompleted
      ? 100
      : isFinalizing
        ? 100
      : Math.round((questionNumber / totalQuestions) * 100)
  const mcq = questionType === QUESTION_TYPE.MULTIPLE_CHOICE
    ? parseMultipleChoice(question.questionOptions)
    : null
  const choiceOptions = mcq?.options || []
  const selectCount = mcq?.selectCount ?? 1
  const ratingMax = questionType === QUESTION_TYPE.RATING
    ? parseRatingMax(question.questionOptions)
    : null
  const imageUploadMax = questionType === QUESTION_TYPE.IMAGE_UPLOAD
    ? parseImageUploadMaxFiles(question.questionOptions)
    : null
  const responsesNeedAuthMedia = form?.responsesPublic === false
  const selectedRating = answer ? Number(answer) : 0
  const responseAnswers = normalizeAnswerList(responseQuestion?.answers)
  const responseMcqResults = responseQuestion?.questionType === QUESTION_TYPE.MULTIPLE_CHOICE
    ? buildMcqResults(responseQuestion.questionOptions, responseQuestion.answers)
    : null
  const responseRatingResults = responseQuestion?.questionType === QUESTION_TYPE.RATING
    ? buildRatingResults(responseQuestion.questionOptions, responseQuestion.answers)
    : null

  useEffect(() => {
    const fill = progressFillRef.current
    if (!fill) return
    animate(fill, {
      width: `${progressPercent}%`,
      duration: 480,
      ease: 'out(2)',
    })
  }, [progressPercent])

  useEffect(() => {
    const panel = questionPanelRef.current
    if (!panel || isLoading || isLoadingResponses) return undefined

    panel.style.opacity = '0'
    panel.style.transform = 'translateX(28px)'

    const anim = animate(panel, {
      opacity: 1,
      translateX: 0,
      duration: 360,
      ease: 'out(2)',
    })

    return () => anim?.pause?.()
  }, [currentIndex, isCompleted, isFinalizing, isLoading, isLoadingResponses, isShowingResponses, responsesIndex])

  useEffect(() => {
    if (!question || questionType !== QUESTION_TYPE.IMAGE_UPLOAD) {
      setPendingUploadFiles([])
      return
    }
    setPendingUploadFiles(uploadFilesByQuestion[question.id] || [])
  }, [question, questionType, uploadFilesByQuestion])

  const finishSurvey = async (allAnswers, uploadFilesMap = uploadFilesByQuestion) => {
    const answerList = Object.entries(allAnswers).map(([questionId, value]) => ({
      questionId: Number(questionId),
      answer: value,
    }))

    const hasUpload = formHasUploadQuestions(questions)

    if (!hasUpload) {
      if (answerList.length > 0) {
        await api.submitAnswers(form.id, { answers: answerList })
      }
      if (typeof window !== 'undefined') {
        window.localStorage.setItem(completedKey(formId), 'true')
      }
      setIsFinalizing(false)
      setIsCompleted(true)
      return
    }

    const uploadQuestions = questions.filter((q) => q.questionType === QUESTION_TYPE.IMAGE_UPLOAD)
    const stagingId = crypto.randomUUID()

    try {
      for (let i = 0; i < uploadQuestions.length; i += 1) {
        const uploadQuestion = uploadQuestions[i]
        const files = uploadFilesMap[uploadQuestion.id]
        const validationError = validateUploadFiles(
          files,
          parseImageUploadMaxFiles(uploadQuestion.questionOptions),
          t,
        )
        if (validationError) {
          throw new Error(validationError)
        }
        setUploadProgress({
          phase: 'upload',
          index: i + 1,
          total: uploadQuestions.length,
          label: uploadQuestion.questionText || t('access.uploadQuestionFallback'),
        })
        await api.stageQuestionFiles(form.id, stagingId, uploadQuestion.id, files)
      }

      setUploadProgress({ phase: 'commit', index: uploadQuestions.length, total: uploadQuestions.length, label: '' })
      await api.commitSubmission(form.id, stagingId, { answers: answerList })

      if (typeof window !== 'undefined') {
        window.localStorage.setItem(completedKey(formId), 'true')
      }
      setUploadProgress(null)
      setIsFinalizing(false)
      setIsCompleted(true)
    } catch (err) {
      setUploadProgress(null)
      setIsFinalizing(false)
      throw err
    }
  }

  const openResponses = useCallback(async () => {
    if (!formId) return

    setError('')
    setIsLoadingResponses(true)
    try {
      const data = await api.getFormResponses(formId)
      setResponsesData(data)
      setResponsesIndex(0)
      setIsViewingResponses(true)
    } catch (err) {
      setError(err.message || t('access.responsesError'))
    } finally {
      setIsLoadingResponses(false)
    }
  }, [formId, t])

  useEffect(() => {
    // Creators opening from /surveys use ?view=responses without having filled the form.
    if (
      !shouldOpenResponses
      || autoOpenedResponsesRef.current
      || isViewingResponses
      || isLoading
      || isLoadingResponses
    ) {
      return
    }
    autoOpenedResponsesRef.current = true
    openResponses()
  }, [shouldOpenResponses, isViewingResponses, isLoading, isLoadingResponses, openResponses])

  const runQuestionExit = () =>
    new Promise((resolve) => {
      const panel = questionPanelRef.current
      if (!panel) {
        resolve()
        return
      }
      animate(panel, {
        opacity: 0,
        translateX: -24,
        duration: 240,
        ease: 'in(2)',
        onComplete: resolve,
      })
    })

  const showSubmittingPanel = () =>
    new Promise((resolve) => {
      setIsFinalizing(true)
      window.setTimeout(resolve, 60)
    })

  const advance = async (nextAnswers, uploadFilesOverride) => {
    await runQuestionExit()
    setAnswers(nextAnswers)
    setAnswer('')
    setSelectedChoices([])

    if (isLastQuestion) {
      await showSubmittingPanel()
      try {
        await finishSurvey(nextAnswers, uploadFilesOverride)
      } catch (err) {
        setIsFinalizing(false)
        throw err
      }
    } else {
      setCurrentIndex((index) => index + 1)
    }
  }

  const toggleChoice = (option) => {
    setError('')
    if (selectCount === 1) {
      setSelectedChoices([option])
      return
    }

    setSelectedChoices((prev) => {
      if (prev.includes(option)) {
        return prev.filter((item) => item !== option)
      }
      if (prev.length >= selectCount) {
        return prev
      }
      return [...prev, option]
    })
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!form || !question || isCompleted) return

    // Skippable text: display only — continue without recording an answer.
    if (questionType === QUESTION_TYPE.SKIPPABLE_TEXT) {
      setError('')
      setIsSubmitting(true)
      try {
        await advance(answers)
      } catch (err) {
        setError(err.message || t('access.continueError'))
      } finally {
        setIsSubmitting(false)
      }
      return
    }

    if (questionType === QUESTION_TYPE.IMAGE_UPLOAD) {
      const maxFiles = imageUploadMax ?? 1
      const validationError = validateUploadFiles(pendingUploadFiles, maxFiles, t)
      if (validationError) {
        setError(validationError)
        return
      }

      setError('')
      setIsSubmitting(true)
      try {
        const nextUploadFiles = {
          ...uploadFilesByQuestion,
          [question.id]: pendingUploadFiles,
        }
        setUploadFilesByQuestion(nextUploadFiles)
        await advance(answers, nextUploadFiles)
      } catch (err) {
        setError(err.message || t('access.submitError'))
      } finally {
        setIsSubmitting(false)
      }
      return
    }

    if (questionType === QUESTION_TYPE.MULTIPLE_CHOICE) {
      if (!mcq || choiceOptions.length === 0) {
        setError(t('access.invalidMcq'))
        return
      }
      if (selectedChoices.length !== selectCount) {
        setError(selectCount === 1
          ? t('access.selectOne')
          : t('access.selectExact', { count: selectCount }))
        return
      }

      setError('')
      setIsSubmitting(true)
      try {
        await advance({
          ...answers,
          [question.id]: selectedChoices.join(';'),
        })
      } catch (err) {
        setError(err.message || t('access.submitError'))
      } finally {
        setIsSubmitting(false)
      }
      return
    }

    const trimmed = answer.trim()
    if (!trimmed) {
      if (questionType === QUESTION_TYPE.RATING) {
        setError(t('access.selectRating'))
      } else {
        setError(t('access.enterAnswer'))
      }
      return
    }

    setError('')
    setIsSubmitting(true)

    try {
      await advance({
        ...answers,
        [question.id]: trimmed,
      })
    } catch (err) {
      setError(err.message || t('access.submitError'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const primaryLabel = () => {
    if (isSubmitting) return t('access.submitting')
    if (questionType === QUESTION_TYPE.SKIPPABLE_TEXT) {
      return isLastQuestion ? t('access.finish') : t('access.next')
    }
    return isLastQuestion ? t('access.submitAnswers') : t('access.nextQuestion')
  }

  const choiceDisabled =
    (questionType === QUESTION_TYPE.MULTIPLE_CHOICE && (!mcq || choiceOptions.length === 0))
    || (questionType === QUESTION_TYPE.RATING && ratingMax == null)
    || (questionType === QUESTION_TYPE.IMAGE_UPLOAD && imageUploadMax == null)

  const onUploadFilesSelected = (event) => {
    const selected = Array.from(event.target.files || [])
    event.target.value = ''
    setError('')
    const maxFiles = imageUploadMax ?? 1
    const validationError = validateUploadFiles(selected, maxFiles, t)
    if (validationError) {
      setError(validationError)
      return
    }
    setPendingUploadFiles(selected)
  }

  const removePendingUpload = (index) => {
    setPendingUploadFiles((prev) => prev.filter((_, i) => i !== index))
  }

  return (
    <div className="container">
      <div className="py-4">
        <section className="background-radial-gradient overflow-hidden">
          <style>{`
    .background-radial-gradient {
      background-color: rgba(255, 255, 255, 0.18);
      background-image: none;
      backdrop-filter: saturate(160%) blur(28px);
      -webkit-backdrop-filter: saturate(160%) blur(28px);
      border: 1px solid rgba(255, 255, 255, 0.45);
      box-shadow: 0 8px 40px color-mix(in srgb, var(--accent) 8%, transparent);
      border-radius: 8px;
      padding: 0 24px 24px;
    }

    .bg-glass {
      background-color: rgba(255, 255, 255, 0.28) !important;
      backdrop-filter: saturate(180%) blur(24px);
      -webkit-backdrop-filter: saturate(180%) blur(24px);
      border: 1px solid rgba(255, 255, 255, 0.5);
      box-shadow: 0 8px 32px color-mix(in srgb, var(--accent) 6%, transparent);
    }

    .survey-progress-wrap {
      padding: 16px 0 0;
      margin-bottom: 4px;
    }

    .survey-progress {
      height: 6px;
      border-radius: 999px;
      background: color-mix(in srgb, var(--accent) 12%, transparent);
      overflow: hidden;
    }

    .survey-progress-fill {
      height: 100%;
      width: 0%;
      border-radius: inherit;
      background: linear-gradient(90deg, var(--accent), var(--accent-bright));
    }

    .survey-progress-label {
      display: block;
      margin-top: 8px;
      font-size: 0.8rem;
      font-weight: 600;
      color: var(--app-muted, #6c757d);
      letter-spacing: 0.02em;
    }

    .question-panel {
      will-change: transform, opacity;
    }

    .results-bar {
      height: 10px;
      border-radius: 999px;
      overflow: hidden;
      background: color-mix(in srgb, var(--accent) 12%, transparent);
    }

    .results-bar-fill {
      height: 100%;
      border-radius: inherit;
      background: linear-gradient(90deg, var(--accent), var(--accent-bright));
    }

    .results-answer {
      border: 1px solid color-mix(in srgb, var(--accent) 12%, transparent);
      border-radius: 10px;
      background: rgba(255, 255, 255, 0.18);
    }

    .results-upload-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
      gap: 0.75rem;
    }

    .results-upload-thumb {
      width: 100%;
      aspect-ratio: 1;
      object-fit: cover;
      border-radius: 10px;
      border: 1px solid color-mix(in srgb, var(--accent) 12%, transparent);
    }

    .results-upload-thumb--loading {
      background: color-mix(in srgb, var(--accent) 8%, transparent);
      border-radius: 10px;
      aspect-ratio: 1;
    }

    .upload-dropzone {
      border: 2px dashed color-mix(in srgb, var(--accent) 35%, transparent);
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.12);
    }

    .choice-option {
      border: 1px solid color-mix(in srgb, var(--accent) 20%, transparent);
      border-radius: 8px;
      padding: 0.75rem 1rem;
      cursor: pointer;
      transition: background-color 0.15s ease, border-color 0.15s ease;
    }

    .choice-option:hover,
    .choice-option.is-selected {
      border-color: var(--accent);
      background-color: color-mix(in srgb, var(--accent) 6%, transparent);
    }

    .rating-option {
      min-width: 2.75rem;
      justify-content: center;
    }

    .star-button {
      border: none;
      background: transparent;
      padding: 0.15rem 0.2rem;
      font-size: 1.75rem;
      line-height: 1;
      color: #d0d0d8;
      cursor: pointer;
    }

    .star-button.is-filled {
      color: #f5a623;
    }

    .star-button:hover,
    .star-button:focus-visible {
      color: #f5a623;
      outline: none;
    }
  `}</style>

          {(totalQuestions > 0 || isCompleted || isFinalizing || isShowingResponses) && !isLoading && (
            <div className="survey-progress-wrap">
              <div
                className="survey-progress"
                role="progressbar"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={progressPercent}
                aria-label={t('access.progress')}
              >
                <div ref={progressFillRef} className="survey-progress-fill" />
              </div>
              <span className="survey-progress-label">
                {isShowingResponses
                  ? t('access.responsesOf', {
                      n: responseQuestionNumber,
                      total: responseQuestions.length,
                    })
                  : isCompleted
                  ? t('access.complete')
                  : isFinalizing
                    ? t('access.submittingAnswers')
                  : t('access.questionOf', { n: questionNumber, total: totalQuestions })}
              </span>
            </div>
          )}

          <div className="container px-2 px-sm-4 py-3 py-md-5 px-md-5 text-center text-lg-start my-3 my-md-5">
            <div className="row gx-lg-5 align-items-center mb-3 mb-md-5">
              <div className="col-lg-6 mb-4 mb-lg-0 d-flex flex-column justify-content-center align-items-center text-center" style={{ zIndex: 10 }}>
                <h1 className="my-3 my-md-5 display-5 fw-bold ls-tight welcome-heading">
                  {isLoading
                    ? t('access.loadingSurvey')
                    : isShowingResponses
                      ? t('access.surveyResponses', { name: form?.name || t('access.survey') })
                      : (form?.name || (isCompleted ? t('access.surveyComplete') : t('access.survey')))}
                </h1>
                {form?.creatorUsername && !isCompleted && !isFinalizing && !isShowingResponses && (
                  <p className="lead text-muted mb-0">
                    {t('access.createdBy', { name: form.creatorUsername })}
                  </p>
                )}
              </div>

              <div className="col-lg-6 mb-4 mb-lg-0 position-relative">
                <div className="card bg-glass position-relative">
                  <div className="card-body px-3 py-4 px-md-5 py-md-5 pb-4 pb-md-5">
                    {error && (
                      <div className="alert alert-danger" role="alert">{error}</div>
                    )}

                    {isLoading ? (
                      <p className="text-muted text-center mb-0">{t('access.loadingQuestion')}</p>
                    ) : isLoadingResponses ? (
                      <p className="text-muted text-center mb-0">{t('access.loadingResponses')}</p>
                    ) : (
                      <div ref={questionPanelRef} className="question-panel">
                        {isShowingResponses ? (
                          <div>
                            <p className="fw-semibold mb-4">{responseQuestion?.questionText}</p>

                            {responseQuestion?.questionType === QUESTION_TYPE.SKIPPABLE_TEXT && (
                              <p className="text-muted mb-0">{t('access.displayOnly')}</p>
                            )}

                            {responseQuestion?.questionType === QUESTION_TYPE.TEXT && (
                              responseAnswers.length > 0 ? (
                                <div className="d-flex flex-column gap-2 mb-4">
                                  {responseAnswers.map((value, index) => (
                                    <div key={`${responseQuestion.id}-${index}`} className="results-answer p-3">
                                      {value}
                                    </div>
                                  ))}
                                </div>
                              ) : (
                                <p className="text-muted mb-4">{t('access.noTextResponses')}</p>
                              )
                            )}

                            {responseQuestion?.questionType === QUESTION_TYPE.MULTIPLE_CHOICE && (
                              responseMcqResults ? (
                                <div className="d-flex flex-column gap-3 mb-4">
                                  {responseMcqResults.items.map((item) => (
                                    <div key={item.option}>
                                      <div className="d-flex justify-content-between gap-3 mb-1">
                                        <span>{item.option}</span>
                                        <span className="text-muted small">{item.percent}% ({item.count})</span>
                                      </div>
                                      <div className="results-bar">
                                        <div className="results-bar-fill" style={{ width: `${item.percent}%` }} />
                                      </div>
                                    </div>
                                  ))}
                                  <p className="text-muted small mb-0">
                                    {t('access.totalResponses', { count: responseMcqResults.totalResponses })}
                                  </p>
                                </div>
                              ) : (
                                <p className="text-muted mb-4">{t('access.invalidMcqResults')}</p>
                              )
                            )}

                            {responseQuestion?.questionType === QUESTION_TYPE.RATING && (
                              responseRatingResults ? (
                                <div className="d-flex flex-column gap-3 mb-4">
                                  {responseRatingResults.items.map((item) => (
                                    <div key={item.value}>
                                      <div className="d-flex justify-content-between gap-3 mb-1">
                                        <span>
                                          {item.value === 1
                                            ? t('access.starLabel', { n: item.value })
                                            : t('access.starsLabel', { n: item.value })}
                                        </span>
                                        <span className="text-muted small">{item.percent}% ({item.count})</span>
                                      </div>
                                      <div className="results-bar">
                                        <div className="results-bar-fill" style={{ width: `${item.percent}%` }} />
                                      </div>
                                    </div>
                                  ))}
                                  <p className="text-muted small mb-0">
                                    {t('access.totalResponses', { count: responseRatingResults.totalResponses })}
                                  </p>
                                </div>
                              ) : (
                                <p className="text-muted mb-4">{t('access.invalidRating')}</p>
                              )
                            )}

                            {responseQuestion?.questionType === QUESTION_TYPE.IMAGE_UPLOAD && (
                              (() => {
                                const imageNames = responseAnswers.flatMap((value) => splitUploadFileNames(value))
                                if (imageNames.length === 0) {
                                  return <p className="text-muted mb-4">{t('access.noUploadResponses')}</p>
                                }
                                return (
                                  <div className="results-upload-grid mb-4">
                                    {imageNames.map((fileName) => (
                                      <MediaThumbnail
                                        key={`${responseQuestion.id}-${fileName}`}
                                        formId={formId}
                                        fileName={fileName}
                                        authenticated={responsesNeedAuthMedia}
                                      />
                                    ))}
                                  </div>
                                )
                              })()
                            )}

                            <div className="d-flex flex-wrap gap-2 align-items-center mb-0" style={{ marginTop: 8 }}>
                              <button
                                type="button"
                                className="btn btn-outline-primary"
                                onClick={() => setResponsesIndex((index) => Math.max(0, index - 1))}
                                disabled={responsesIndex === 0}
                              >
                                {t('access.previous')}
                              </button>
                              <button
                                type="button"
                                className="btn btn-primary"
                                onClick={() => setResponsesIndex((index) => Math.min(responseQuestions.length - 1, index + 1))}
                                disabled={responsesIndex >= responseQuestions.length - 1}
                              >
                                {t('access.nextResponse')}
                              </button>
                              <button
                                type="button"
                                className="btn btn-link text-decoration-none px-0"
                                onClick={() => {
                                  if (shouldOpenResponses) {
                                    navigate('/surveys')
                                    return
                                  }
                                  setIsViewingResponses(false)
                                }}
                              >
                                {t('common.back')}
                              </button>
                            </div>
                          </div>
                        ) : isCompleted ? (
                          <div className="text-center">
                            <h2 className="fw-bold mb-3 welcome-heading">{t('access.thankYou')}</h2>
                            <p className="text-muted mb-0">
                              {t('access.thankYouClose')}
                            </p>
                            {form?.responsesPublic !== false && (
                              <div className="d-flex justify-content-center mt-4">
                                <button
                                  type="button"
                                  className="btn btn-outline-primary"
                                  onClick={openResponses}
                                  disabled={isLoadingResponses}
                                >
                                  {t('access.viewResponses')}
                                </button>
                              </div>
                            )}
                            {form?.responsesPublic === false && (
                              <p className="text-muted small mt-4 mb-0">
                                {t('access.responsesPrivateNote')}
                              </p>
                            )}
                          </div>
                        ) : isFinalizing ? (
                          <div className="text-center">
                            <h2 className="fw-bold mb-3 welcome-heading">{t('access.submittingTitle')}</h2>
                            {uploadProgress?.phase === 'upload' && (
                              <>
                                <p className="text-muted mb-2">
                                  {t('access.uploadProgressQuestion', {
                                    index: uploadProgress.index,
                                    total: uploadProgress.total,
                                    label: uploadProgress.label,
                                  })}
                                </p>
                                <div className="progress mb-2" style={{ height: 8 }}>
                                  <div
                                    className="progress-bar"
                                    role="progressbar"
                                    style={{
                                      width: `${Math.round((uploadProgress.index / uploadProgress.total) * 100)}%`,
                                    }}
                                  />
                                </div>
                              </>
                            )}
                            {uploadProgress?.phase === 'commit' && (
                              <p className="text-muted mb-0">{t('access.uploadProgressCommit')}</p>
                            )}
                            {!uploadProgress && (
                              <p className="text-muted mb-0">
                                {t('access.submittingWait')}
                              </p>
                            )}
                          </div>
                        ) : question ? (
                          <form onSubmit={handleSubmit}>
                            <p className="fw-semibold mb-4">{question.questionText}</p>

                            {questionType === QUESTION_TYPE.SKIPPABLE_TEXT && (
                              <p className="text-muted mb-4">
                                {t('access.infoContinue')}
                              </p>
                            )}

                            {questionType === QUESTION_TYPE.TEXT && (
                              <div data-mdb-input-init className="form-outline mb-4">
                                <textarea
                                  id="txtAnswer"
                                  className="form-control"
                                  rows={4}
                                  value={answer}
                                  onChange={(e) => setAnswer(e.target.value)}
                                  required
                                />
                                <label className="form-label" htmlFor="txtAnswer">{t('access.yourAnswer')}</label>
                              </div>
                            )}

                            {questionType === QUESTION_TYPE.MULTIPLE_CHOICE && (
                              <div
                                className="d-flex flex-column gap-2 mb-4"
                                role={selectCount === 1 ? 'radiogroup' : 'group'}
                                aria-label={t('access.answerOptions')}
                              >
                                {!mcq || choiceOptions.length === 0 ? (
                                  <p className="text-muted mb-0">
                                    {t('access.invalidOptionsHint')}
                                  </p>
                                ) : (
                                  <>
                                    <p className="text-muted small mb-1">
                                      {selectCount === 1
                                        ? t('access.selectOneHint')
                                        : t('access.selectExactHint', {
                                            count: selectCount,
                                            selected: selectedChoices.length,
                                          })}
                                    </p>
                                    {choiceOptions.map((option) => {
                                      const inputId = `choice-${question.id}-${option}`
                                      const selected = selectedChoices.includes(option)
                                      const inputType = selectCount === 1 ? 'radio' : 'checkbox'
                                      return (
                                        <label
                                          key={option}
                                          htmlFor={inputId}
                                          className={`choice-option d-flex align-items-center gap-2 mb-0 ${selected ? 'is-selected' : ''}`}
                                        >
                                          <input
                                            type={inputType}
                                            id={inputId}
                                            name={`question-${question.id}`}
                                            value={option}
                                            checked={selected}
                                            onChange={() => toggleChoice(option)}
                                          />
                                          <span>{option}</span>
                                        </label>
                                      )
                                    })}
                                  </>
                                )}
                              </div>
                            )}

                            {questionType === QUESTION_TYPE.RATING && (
                              <div className="mb-4" role="radiogroup" aria-label={t('access.starRating')}>
                                {ratingMax == null ? (
                                  <p className="text-muted mb-0">
                                    {t('access.invalidRatingHint', {
                                      min: RATING_MAX_MIN,
                                      max: RATING_MAX_MAX,
                                    })}
                                  </p>
                                ) : (
                                  <>
                                    <div className="d-flex flex-wrap align-items-center gap-1 mb-2">
                                      {Array.from({ length: ratingMax }, (_, i) => i + 1).map((value) => {
                                        const filled = value <= selectedRating
                                        return (
                                          <button
                                            key={value}
                                            type="button"
                                            className={`star-button ${filled ? 'is-filled' : ''}`}
                                            aria-label={
                                              value === 1
                                                ? t('access.starLabel', { n: value })
                                                : t('access.starsLabel', { n: value })
                                            }
                                            aria-pressed={answer === String(value)}
                                            onClick={() => setAnswer(String(value))}
                                          >
                                            {filled ? '★' : '☆'}
                                          </button>
                                        )
                                      })}
                                    </div>
                                    <p className="text-muted small mb-0">
                                      {selectedRating > 0
                                        ? `${selectedRating} / ${ratingMax}`
                                        : t('access.selectStars', { max: ratingMax })}
                                    </p>
                                  </>
                                )}
                              </div>
                            )}

                            {questionType === QUESTION_TYPE.IMAGE_UPLOAD && (
                              <div className="mb-4">
                                {imageUploadMax == null ? (
                                  <p className="text-muted mb-0">{t('access.invalidUploadConfig')}</p>
                                ) : (
                                  <>
                                    <p className="text-muted small mb-2">
                                      {t('access.uploadHint', { max: imageUploadMax })}
                                    </p>
                                    <div
                                      className="upload-dropzone mb-3 p-4 text-center"
                                      onDragOver={(e) => e.preventDefault()}
                                      onDrop={(e) => {
                                        e.preventDefault()
                                        const dropped = Array.from(e.dataTransfer.files || [])
                                        const validationError = validateUploadFiles(dropped, imageUploadMax, t)
                                        if (validationError) {
                                          setError(validationError)
                                          return
                                        }
                                        setError('')
                                        setPendingUploadFiles(dropped)
                                      }}
                                    >
                                      <input
                                        id={`upload-${question.id}`}
                                        type="file"
                                        accept="image/jpeg,image/png,image/webp,image/gif"
                                        multiple={imageUploadMax > 1}
                                        className="d-none"
                                        onChange={onUploadFilesSelected}
                                      />
                                      <label htmlFor={`upload-${question.id}`} className="btn btn-outline-primary mb-0">
                                        {t('access.chooseImages')}
                                      </label>
                                      <p className="text-muted small mt-2 mb-0">{t('access.uploadDropHint')}</p>
                                    </div>
                                    {pendingUploadFiles.length > 0 && (
                                      <ul className="list-group mb-0">
                                        {pendingUploadFiles.map((file, index) => (
                                          <li
                                            key={`${file.name}-${file.size}-${index}`}
                                            className="list-group-item d-flex justify-content-between align-items-center gap-2"
                                          >
                                            <span className="text-truncate">{file.name}</span>
                                            <button
                                              type="button"
                                              className="btn btn-sm btn-outline-secondary"
                                              onClick={() => removePendingUpload(index)}
                                            >
                                              {t('access.removeFile')}
                                            </button>
                                          </li>
                                        ))}
                                      </ul>
                                    )}
                                  </>
                                )}
                              </div>
                            )}

                            <div className="d-flex flex-wrap gap-2 justify-content-start align-items-center mb-3" style={{ marginTop: 8 }}>
                              <button
                                type="submit"
                                data-mdb-button-init
                                data-mdb-ripple-init
                                className="btn btn-primary login"
                                disabled={isSubmitting || choiceDisabled}
                              >
                                {primaryLabel()}
                              </button>
                            </div>
                          </form>
                        ) : null}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
