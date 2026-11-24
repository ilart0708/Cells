import { request } from './http'

// Member: report a fellow member's answer in one of my rooms. Reporting twice is harmless.
export const reportAnswer = (roomId, submissionId, reason, note) =>
  request(`/member/rooms/${roomId}/submissions/${submissionId}/report`, {
    method: 'POST',
    body: { reason, note: note || null },
  })

// Leader: open reports about answers in my room (the reporter is never named)
export const listReports = (roomId) => request(`/leader/rooms/${roomId}/reports`)

// Leader: "keep the answer"
export const dismissReport = (roomId, reportId) =>
  request(`/leader/rooms/${roomId}/reports/${reportId}/dismiss`, { method: 'POST' })

// Leader: replace the answer's text; every open report about it closes
export const removeReportedAnswer = (roomId, reportId) =>
  request(`/leader/rooms/${roomId}/reports/${reportId}/remove-answer`, { method: 'POST' })
