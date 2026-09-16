/**
 * Translation helpers that optionally scramble when the locale string changes.
 */
import React, { useContext } from 'react'
import ScrambleText from '../animations/ScrambleText'
import { LocaleContext } from './context'

/**
 * Looks up a translation key and renders it with the language scramble effect.
 * Use for short visible labels (nav, headings, buttons). Prefer plain `t()` for
 * aria-labels, placeholders, and long body copy you do not want animated.
 *
 * @example <T k="nav.surveys" />
 * @example <T k="dashboard.welcome" vars={{ name: ' Ada' }} as="span" />
 */
export function T({
  k,
  vars,
  as = 'span',
  scramble = true,
  className,
  style,
  scrambleOptions,
  ...rest
}) {
  const { t } = useContext(LocaleContext)
  const text = t(k, vars)

  if (!scramble) {
    return text
  }

  return (
    <ScrambleText
      as={as}
      text={text}
      className={className}
      style={style}
      scrambleOptions={scrambleOptions}
      {...rest}
    />
  )
}
