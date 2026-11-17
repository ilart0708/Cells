import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import MyLeadersView from '../MyLeadersView.vue'
import { ApiError } from '../../../api/http'
import * as api from '../../../api/pastorArea'

vi.mock('../../../api/pastorArea', () => ({
  listMyLeaders: vi.fn(),
  setMyLeaderStatus: vi.fn(),
  inviteLeader: vi.fn(),
  listMyInvitations: vi.fn(),
  revokeMyInvitation: vi.fn(),
}))

// jsdom has the <dialog> element but not showModal()/close().
beforeAll(() => {
  HTMLDialogElement.prototype.showModal = function () {
    this.setAttribute('open', '')
  }
  HTMLDialogElement.prototype.close = function () {
    this.removeAttribute('open')
    this.dispatchEvent(new Event('close'))
  }
})

const empty = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
const pageOf = (content) => ({ ...empty, content, totalElements: content.length, totalPages: 1 })

const leader = {
  id: 11,
  fullName: 'Leader Amy',
  email: 'amy@test.com',
  status: 'ACTIVE',
  churchId: 1,
  churchName: 'Victory Church',
  pastorId: 2,
  pastorName: 'Pastor John',
  createdAt: '2026-01-05T00:00:00Z',
}

const created = {
  id: 4,
  email: 'new@test.com',
  fullName: 'New Leader',
  role: 'LEADER',
  churchId: 1,
  churchName: 'Victory Church',
  status: 'PENDING',
  expiresAt: '2026-12-01T00:00:00Z',
  createdAt: '2026-11-24T00:00:00Z',
  inviteUrl: 'http://localhost:5173/accept-invitation#token=SECRET',
}

const buttonWithText = (wrapper, text) => wrapper.findAll('button').find((b) => b.text().includes(text))

async function mountView() {
  const wrapper = mount(MyLeadersView)
  await flushPromises()
  return wrapper
}

async function fillAndSubmit(wrapper, name, email) {
  await buttonWithText(wrapper, 'Invite leader').trigger('click')
  await wrapper.get('dialog input[type="text"]').setValue(name)
  await wrapper.get('dialog input[type="email"]').setValue(email)
  await wrapper.get('dialog form').trigger('submit')
  await flushPromises()
}

describe('MyLeadersView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    api.listMyLeaders.mockResolvedValue(pageOf([leader]))
    api.listMyInvitations.mockResolvedValue(empty)
  })

  it('lists the pastor’s leaders', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Leader Amy')
    expect(wrapper.text()).toContain('Victory Church')
    expect(wrapper.text()).toContain('Active')
  })

  it('invites the pastor to add a first leader when there are none', async () => {
    api.listMyLeaders.mockResolvedValue(empty)

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('You have no leaders yet')
  })

  it('invites a leader with only a name and an email, never a church', async () => {
    api.inviteLeader.mockResolvedValue(created)
    const wrapper = await mountView()

    await fillAndSubmit(wrapper, '  New Leader ', ' new@test.com ')

    expect(api.inviteLeader).toHaveBeenCalledWith({ email: 'new@test.com', fullName: 'New Leader' })
  })

  it('shows the invitation link once, and refreshes the invitations when closed', async () => {
    api.inviteLeader.mockResolvedValue(created)
    const wrapper = await mountView()

    await fillAndSubmit(wrapper, 'New Leader', 'new@test.com')
    expect(wrapper.get('#invite-link').element.value).toBe(created.inviteUrl)

    const before = api.listMyInvitations.mock.calls.length
    await buttonWithText(wrapper, 'Done').trigger('click')
    await flushPromises()

    expect(api.listMyInvitations.mock.calls.length).toBe(before + 1)
    expect(wrapper.find('#invite-link').exists()).toBe(false)
    expect(wrapper.text()).toContain('Invitation created for new@test.com.')
  })

  it('does not call the server when the form is incomplete', async () => {
    const wrapper = await mountView()

    await buttonWithText(wrapper, 'Invite leader').trigger('click')
    await wrapper.get('dialog form').trigger('submit')
    await flushPromises()

    expect(api.inviteLeader).not.toHaveBeenCalled()
    expect(wrapper.get('dialog').text()).toContain('Please enter the leader’s name.')
    expect(wrapper.get('dialog').text()).toContain('Please enter an email address.')
  })

  it('shows the server message under the email when the address is already used', async () => {
    api.inviteLeader.mockRejectedValue(new ApiError(409, { detail: 'A user with this email already exists.' }))
    const wrapper = await mountView()

    await fillAndSubmit(wrapper, 'Leader Amy', 'amy@test.com')

    expect(wrapper.get('dialog').text()).toContain('A user with this email already exists.')
  })

  it('shows a general error when the pastor has no church', async () => {
    api.inviteLeader.mockRejectedValue(
      new ApiError(422, { detail: 'Your account is not linked to a church.' }),
    )
    const wrapper = await mountView()

    await fillAndSubmit(wrapper, 'New Leader', 'new@test.com')

    expect(wrapper.get('dialog').text()).toContain('Your account is not linked to a church.')
  })

  it('asks for confirmation, then disables the leader', async () => {
    api.setMyLeaderStatus.mockResolvedValue({})
    const wrapper = await mountView()

    await buttonWithText(wrapper, 'Disable').trigger('click')
    expect(api.setMyLeaderStatus).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Disable Leader Amy?')

    await wrapper.get('dialog button.bg-navy-900').trigger('click')
    await flushPromises()

    expect(api.setMyLeaderStatus).toHaveBeenCalledWith(11, 'DISABLED')
    expect(wrapper.text()).toContain('Leader Amy is now disabled.')
  })

  it('revokes a pending invitation after confirmation', async () => {
    api.listMyInvitations.mockResolvedValue(pageOf([created]))
    api.revokeMyInvitation.mockResolvedValue(null)
    const wrapper = await mountView()

    await buttonWithText(wrapper, 'Revoke').trigger('click')
    await wrapper.get('dialog button.bg-navy-900').trigger('click')
    await flushPromises()

    expect(api.revokeMyInvitation).toHaveBeenCalledWith(4)
  })
})
