import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import PastorManagementView from '../PastorManagementView.vue'
import * as churchesApi from '../../../api/churches'
import * as invitationsApi from '../../../api/adminInvitations'
import * as pastorsApi from '../../../api/pastors'

vi.mock('../../../api/churches', () => ({ listChurches: vi.fn() }))
vi.mock('../../../api/pastors', () => ({ listPastors: vi.fn(), setPastorStatus: vi.fn() }))
vi.mock('../../../api/adminInvitations', () => ({
  listInvitations: vi.fn(),
  invitePastor: vi.fn(),
  revokeInvitation: vi.fn(),
}))

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

const pastor = {
  id: 7,
  fullName: 'Pastor John',
  email: 'john@test.com',
  status: 'ACTIVE',
  churchId: 1,
  churchName: 'Victory Church',
  createdAt: '2026-01-05T00:00:00Z',
}

const created = {
  id: 3,
  email: 'new@test.com',
  fullName: 'New Pastor',
  role: 'PASTOR',
  churchId: 1,
  churchName: 'Victory Church',
  status: 'PENDING',
  expiresAt: '2026-12-01T00:00:00Z',
  createdAt: '2026-11-24T00:00:00Z',
  inviteUrl: 'http://localhost:5173/accept-invitation#token=SECRET',
}

const buttonWithText = (wrapper, text) => wrapper.findAll('button').find((b) => b.text().includes(text))

async function mountView() {
  const wrapper = mount(PastorManagementView, {
    global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } },
  })
  await flushPromises()
  return wrapper
}

describe('PastorManagementView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    churchesApi.listChurches.mockResolvedValue(
      pageOf([{ id: 1, name: 'Victory Church' }, { id: 2, name: 'Grace Church' }]),
    )
    pastorsApi.listPastors.mockResolvedValue(pageOf([pastor]))
    invitationsApi.listInvitations.mockResolvedValue(empty)
  })

  it('lists pastors with their church and status', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Pastor John')
    expect(wrapper.text()).toContain('Victory Church')
    expect(wrapper.text()).toContain('Active')
  })

  it('asks for confirmation, then disables the pastor', async () => {
    pastorsApi.setPastorStatus.mockResolvedValue({})
    const wrapper = await mountView()

    await buttonWithText(wrapper, 'Disable').trigger('click')
    expect(pastorsApi.setPastorStatus).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Disable Pastor John?')

    await wrapper.get('dialog button.bg-navy-900').trigger('click')
    await flushPromises()

    expect(pastorsApi.setPastorStatus).toHaveBeenCalledWith(7, 'DISABLED')
    expect(wrapper.text()).toContain('Pastor John is now disabled.')
  })

  it('shows the invitation link once, and refreshes the list when closed', async () => {
    invitationsApi.invitePastor.mockResolvedValue(created)
    const wrapper = await mountView()

    await buttonWithText(wrapper, 'Invite pastor').trigger('click')
    await wrapper.get('dialog input[type="text"]').setValue('New Pastor')
    await wrapper.get('dialog input[type="email"]').setValue('new@test.com')
    await wrapper.get('dialog select').setValue('1')
    await wrapper.get('dialog form').trigger('submit')
    await flushPromises()

    expect(invitationsApi.invitePastor).toHaveBeenCalledWith({
      email: 'new@test.com',
      fullName: 'New Pastor',
      churchId: 1,
    })
    expect(wrapper.get('#invite-link').element.value).toBe(created.inviteUrl)

    const before = invitationsApi.listInvitations.mock.calls.length
    await buttonWithText(wrapper, 'Done').trigger('click')
    await flushPromises()

    expect(invitationsApi.listInvitations.mock.calls.length).toBe(before + 1)
    expect(wrapper.find('#invite-link').exists()).toBe(false)
  })

  it('does not call the server when the form is incomplete', async () => {
    const wrapper = await mountView()

    await buttonWithText(wrapper, 'Invite pastor').trigger('click')
    await wrapper.get('dialog form').trigger('submit')
    await flushPromises()

    expect(invitationsApi.invitePastor).not.toHaveBeenCalled()
    expect(wrapper.get('dialog').text()).toContain('Please choose a church.')
  })

  it('revokes a pending invitation after confirmation', async () => {
    invitationsApi.listInvitations.mockResolvedValue(pageOf([created]))
    invitationsApi.revokeInvitation.mockResolvedValue(null)
    const wrapper = await mountView()

    await buttonWithText(wrapper, 'Revoke').trigger('click')
    await wrapper.get('dialog button.bg-navy-900').trigger('click')
    await flushPromises()

    expect(invitationsApi.revokeInvitation).toHaveBeenCalledWith(3)
  })
})
