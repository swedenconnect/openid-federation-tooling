/**
 * Extracts a human-readable message from a failed fetch's response body. Backend errors are typically Spring
 * ProblemDetail JSON (`{ detail, title, ... }`); this returns `detail` (or `title`) when present, and falls back
 * to the raw response text when it isn't JSON at all.
 *
 * @param {string} responseText the raw response body of a failed request
 * @returns {string} a clean message suitable for display, or an empty string if responseText is empty
 */
export function extractErrorDetail(responseText) {
  if (!responseText) {
    return ''
  }
  try {
    const problem = JSON.parse(responseText)
    return problem.detail || problem.title || responseText
  } catch {
    return responseText
  }
}
