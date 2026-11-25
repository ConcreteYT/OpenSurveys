import React from 'react'
import { Link } from 'react-router-dom'
import { T } from '../i18n'

export default function LegalFooter() {
  return (
    <footer className="legal-footer">
      <Link to="/impressum">
        <T k="legal.impressum" />
      </Link>
      <Link to="/datenschutz">
        <T k="legal.privacy" />
      </Link>
    </footer>
  )
}
