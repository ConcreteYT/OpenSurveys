/**
 * Renders one legal document (Impressum or Datenschutzerklärung) for the active locale.
 * Bracketed placeholders and URLs are marked so the operator can find them.
 */
import React from 'react'
import { useLocale, useT } from '../i18n'
import { legalDocuments } from '../content/legal'

const TOKEN = /(\[[^\]]+\]|https?:\/\/[^\s]+)/g

function RichText({ text }) {
  const parts = text.split(TOKEN)
  return parts.map((part, index) => {
    if (part.startsWith('[')) {
      return (
        <span key={index} className="legal-placeholder">
          {part}
        </span>
      )
    }
    if (part.startsWith('http://') || part.startsWith('https://')) {
      return (
        <a key={index} href={part} target="_blank" rel="noopener noreferrer">
          {part}
        </a>
      )
    }
    return <React.Fragment key={index}>{part}</React.Fragment>
  })
}

export default function LegalDocument({ id }) {
  const { locale } = useLocale()
  const t = useT()
  const pack = legalDocuments[locale] || legalDocuments.de
  const doc = pack[id]

  return (
    <article className="legal-page">
      <p className="legal-note">{t('legal.draftNote')}</p>
      <h1>{doc.title}</h1>
      {doc.sections.map((section) => (
        <section key={section.heading}>
          <h2>{section.heading}</h2>
          {section.paragraphs.map((paragraph) => (
            <p key={paragraph}>
              <RichText text={paragraph} />
            </p>
          ))}
        </section>
      ))}
    </article>
  )
}
