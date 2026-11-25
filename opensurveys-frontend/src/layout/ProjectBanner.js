/**
 * Thin notice fixed to the top of every page.
 * Set SHOW_PROJECT_BANNER to false to hide it. The layout gap closes with it.
 */
import React from 'react'
import { useT } from '../i18n'

export const SHOW_PROJECT_BANNER = true

export default function ProjectBanner() {
  const t = useT()

  if (!SHOW_PROJECT_BANNER) return null

  return (
    <div className="project-banner" role="note">
      {t('banner.studentProject')}
    </div>
  )
}
