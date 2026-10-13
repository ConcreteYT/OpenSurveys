import React, { useLayoutEffect, useRef } from 'react'

const DEFAULT_SCRAMBLE = {
  duration: 520,
  chars: 'uppercase',
  perturbation: 0.18,
  from: 'left',
}

const SCRAMBLE_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'

function mixText(from, to, progress) {
  const length = Math.max(from.length, to.length)
  let out = ''
  for (let i = 0; i < length; i += 1) {
    if (progress > i / length) {
      out += to[i] || ''
    } else {
      out += SCRAMBLE_CHARS[Math.floor(Math.random() * SCRAMBLE_CHARS.length)]
    }
  }
  return out
}

function prefersReducedMotion() {
  if (typeof window === 'undefined' || !window.matchMedia) return false
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

/**
 * Renders text and, when the string changes (e.g. language switch),
 * reveals the new copy via anime.js scrambleText instead of a hard swap.
 *
 * Children are intentionally omitted after mount so React does not overwrite
 * the DOM mid-animation; the final string is exposed via aria-label.
 */
export default function ScrambleText({
  text,
  as: Tag = 'span',
  className,
  style,
  scrambleOptions,
  disabled = false,
  ...rest
}) {
  const ref = useRef(null)
  const displayedRef = useRef(typeof text === 'string' ? text : '')
  const isFirstRef = useRef(true)
  const animRef = useRef(null)
  const value = text == null ? '' : String(text)

  useLayoutEffect(() => {
    const el = ref.current
    if (!el) return undefined

    if (isFirstRef.current) {
      isFirstRef.current = false
      el.textContent = value
      displayedRef.current = value
      return undefined
    }

    if (displayedRef.current === value) return undefined

    animRef.current?.pause?.()
    animRef.current = null

    // Keep previous glyph visible so scramble can morph into the new string.
    el.textContent = displayedRef.current

    if (disabled || prefersReducedMotion()) {
      el.textContent = value
      displayedRef.current = value
      return undefined
    }

    const target = value
    const from = displayedRef.current
    displayedRef.current = target
    const duration = (scrambleOptions && scrambleOptions.duration) || DEFAULT_SCRAMBLE.duration
    const start = performance.now()
    let frame = 0

    const tick = (now) => {
      const progress = Math.min(1, (now - start) / duration)
      el.textContent = mixText(from, target, progress)
      if (progress < 1) {
        frame = requestAnimationFrame(tick)
      }
    }
    frame = requestAnimationFrame(tick)
    animRef.current = { pause: () => cancelAnimationFrame(frame) }

    return () => {
      cancelAnimationFrame(frame)
      animRef.current = null
      if (el && displayedRef.current === target) {
        el.textContent = target
      }
    }
    // scrambleOptions is read when text changes; omit from deps to avoid
    // re-scrambling when parents pass a new inline options object each render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, disabled])

  return (
    <Tag
      ref={ref}
      className={className}
      style={style}
      aria-label={value}
      data-scramble-text=""
      {...rest}
    />
  )
}
