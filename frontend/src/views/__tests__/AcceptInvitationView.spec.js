import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import AcceptInvitationView from '../AcceptInvitationView.vue'
import { ApiError } from '../../api/http'
import * as invitationsApi from '../../api/invitations'

vi.mock('../../api/invitations', () => ({
  previewInvitation: vi.fn(),
  acceptInvitation: vi.fn(),
}))

const Blank = { render: () => null }
const GOOD_PASSWORD = 'LongEnough#2026'

const preview = {
  email: 'john@test.com',
  fullName: 'Pastor John',
  role: 'PASTOR',
  churchName: 'Victory Church',
  expiresAt: '2030-01-01T00:00:00Z',
}

async function open(hash = '#token=abc123') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', name: 'login', component: Blank },
      { path: '/accept-invitation', name: 'accept-invitation', component: AcceptInvitationView },
      { path: '/admin', name: 'admin-dashboard', component: Blank },
      { path: '/pastor', name: 'pastor-dashboard', component: Blank },
      { path: '/leader', name: 'leader-dashboard', component: Blank },
      { path: '/member', name: 'member-dashboard', component: Blank },
    ],
  })
  await router.push(`/accept-invitation${hash}`)
  const wrapper = mount(AcceptInvitationView, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return { wrapper, router }
}

async function fillAndSubmit(wrapper, { name = 'Pastor John', password = GOOD_PASSWORD, confirm = GOOD_PASSWORD } = {}) {
  await wrapper.get('#fullName').setValue(name)
  await wrapper.get('#password').setValue(password)
  await wrapper.get('#confirmPassword').setValue(confirm)
  await wrapper.get('form').trigger('submit')
  await flushPromises()
}

describe('AcceptInvitationView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('looks up the invitation with the token from the link fragment', async () => {
    invitationsApi.previewInvitation.mockResolvedValue(preview)

    await open()

    expect(invitationsApi.previewInvitation).toHaveBeenCalledWith('abc123')
  })

  it('shows who is invited and to which church, with a read-only email', async () => {
    invitationsApi.previewInvitation.mockResolvedValue(preview)

    const { wrapper } = await open()

    expect(wrapper.text()).toContain('Victory Church')
    expect(wrapper.text()).toContain('Joining as Pastor')
    expect(wrapper.get('#email').element.value).toBe('john@test.com')
    expect(wrapper.get('#email').attributes('readonly')).toBeDefined()
    expect(wrapper.get('#fullName').element.value).toBe('Pastor John')
  })

  it('removes the token from the address bar', async () => {
    invitationsApi.previewInvitation.mockResolvedValue(preview)

    const { router } = await open()

    expect(router.currentRoute.value.hash).toBe('')
  })

  it('treats a link without a token as invalid, without asking the server', async () => {
    const { wrapper } = await open('')

    expect(wrapper.text()).toContain('invalid or has expired')
    expect(invitationsApi.previewInvitation).not.toHaveBeenCalled()
  })

  it('shows the invalid message when the server answers 410', async () => {
    invitationsApi.previewInvitation.mockRejectedValue(new ApiError(410, { detail: 'gone' }))

    const { wrapper } = await open()

    expect(wrapper.text()).toContain('invalid or has expired')
  })

  it('offers a retry when the server cannot be reached, and still has the token', async () => {
    invitationsApi.previewInvitation
      .mockRejectedValueOnce(new ApiError(0, { detail: 'offline' }))
      .mockResolvedValueOnce(preview)

    const { wrapper } = await open()
    expect(wrapper.text()).toContain('Try again')

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Victory Church')
    expect(invitationsApi.previewInvitation).toHaveBeenCalledTimes(2)
    expect(invitationsApi.previewInvitation).toHaveBeenLastCalledWith('abc123')
  })

  it('rejects a short password without calling the server', async () => {
    invitationsApi.previewInvitation.mockResolvedValue(preview)
    const { wrapper } = await open()

    await fillAndSubmit(wrapper, { password: 'short', confirm: 'short' })

    expect(wrapper.text()).toContain('Password must be 12 to 72 characters.')
    expect(invitationsApi.acceptInvitation).not.toHaveBeenCalled()
  })

  it('rejects passwords that do not match', async () => {
    invitationsApi.previewInvitation.mockResolvedValue(preview)
    const { wrapper } = await open()

    await fillAndSubmit(wrapper, { confirm: 'Something#Else2026' })

    expect(wrapper.text()).toContain('The two passwords do not match.')
    expect(invitationsApi.acceptInvitation).not.toHaveBeenCalled()
  })

  it('creates the account and goes to the area of the invited role', async () => {
    invitationsApi.previewInvitation.mockResolvedValue(preview)
    invitationsApi.acceptInvitation.mockResolvedValue({
      accessToken: 'token-123',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: { id: 5, email: 'john@test.com', fullName: 'Pastor John', role: 'PASTOR' },
    })
    const { wrapper, router } = await open()

    await fillAndSubmit(wrapper)

    expect(invitationsApi.acceptInvitation).toHaveBeenCalledWith('abc123', 'Pastor John', GOOD_PASSWORD)
    expect(router.currentRoute.value.name).toBe('pastor-dashboard')
  })

  it('shows the invalid message when the link was used while the form was open', async () => {
    invitationsApi.previewInvitation.mockResolvedValue(preview)
    invitationsApi.acceptInvitation.mockRejectedValue(new ApiError(410, { detail: 'gone' }))
    const { wrapper } = await open()

    await fillAndSubmit(wrapper)

    expect(wrapper.text()).toContain('invalid or has expired')
  })

  it('shows field errors returned by the server', async () => {
    invitationsApi.previewInvitation.mockResolvedValue(preview)
    invitationsApi.acceptInvitation.mockRejectedValue(
      new ApiError(400, {
        detail: 'Validation failed.',
        errors: { password: 'Password must be 12 to 72 characters.' },
      }),
    )
    const { wrapper } = await open()

    await fillAndSubmit(wrapper)

    expect(wrapper.text()).toContain('Please check the highlighted fields.')
    expect(wrapper.text()).toContain('Password must be 12 to 72 characters.')
  })
})
