import { z } from 'zod'

const optionSchema = z.object({ id: z.string().uuid(), label: z.string(), position: z.number() })
const pollSchema = z.object({ id: z.string().uuid(), title: z.string(), createdAt: z.string(), options: z.array(optionSchema).length(3) })
const resultsSchema = z.object({
  pollId: z.string().uuid(), totalVotes: z.number().nonnegative(), selectedOptionId: z.string().uuid().nullable(),
  options: z.array(optionSchema.extend({ votes: z.number().nonnegative(), percentage: z.number().min(0).max(100) })).length(3)
})
const createdPollSchema = z.object({ id: z.string().uuid(), url: z.string() })

export type Poll = z.infer<typeof pollSchema>
export type Results = z.infer<typeof resultsSchema>

export class ApiError extends Error {
  constructor(public readonly code: string, message: string, public readonly status: number) { super(message) }
}

async function request(path: string, init?: RequestInit): Promise<unknown> {
  const response = await fetch(`/api${path}`, { ...init, headers: { Accept: 'application/json', ...init?.headers } })
  const body: unknown = await response.json().catch(() => null)
  if (!response.ok) {
    const error = z.object({ code: z.string(), message: z.string() }).safeParse(body)
    throw new ApiError(error.success ? error.data.code : 'REQUEST_FAILED', error.success ? error.data.message : '请求失败，请稍后重试', response.status)
  }
  return body
}

export async function createPoll(title: string, options: string[]) {
  return createdPollSchema.parse(await request('/polls', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ title, options }) }))
}
export async function getPoll(id: string): Promise<Poll> { return pollSchema.parse(await request(`/polls/${id}`)) }
export async function getResults(id: string, visitorId: string): Promise<Results> { return resultsSchema.parse(await request(`/polls/${id}/results`, { headers: { 'X-Visitor-Id': visitorId, 'Cache-Control': 'no-store' } })) }
export async function submitVote(id: string, optionId: string, visitorId: string): Promise<Results> {
  return resultsSchema.parse(await request(`/polls/${id}/votes`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ optionId, visitorId }) }))
}
