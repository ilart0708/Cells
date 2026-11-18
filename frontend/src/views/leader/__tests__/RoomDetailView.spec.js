import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { RouterView, createMemoryHistory, createRouter } from 'vue-router'
import RoomDetailView from '../RoomDetailView.vue'
import { ApiError } from '../../../api/http'
import * as api from '../../../api/roomMembers'
import * as roomsApi from '../../../api/rooms'

vi.mock('../../../api/rooms', () => ({ getRoom: vi.fn() }))
vi.mock('../../../api/roomMembers', () => ({
  getRoster: vi.fn(),
  listAvailableMembers: vi.fn(),
  addMember: vi.fn(),
  saveOrder: vi.fn(),
  removeMember: vi.fn(),
  inviteMember: vi.fn(),
  listRoomInvitations: vi.fn(),
  revokeRoomInvitation: vi.fn(),
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

const room = (overrides = {}) => ({
  id: 5,
  name: 'Youth',
  status: 'ACTIVE',
  churchId: 1,
  churchName: 'Victory Church',
  leaderId: 2,
  leaderName: 'Leader Amy',
  memberCount: 3,
  createdAt: '2026-01-05T00:00:00Z',
  archivedAt: null,
  ...overrides,
})

const person = (id, name, number) => ({
  memberId: id,
  fullName: name,
  email: `${name.toLowerCase()}@test.com`,
  accountStatus: 'ACTIVE',
  participationNumber: number,
  joinedAt: '2026-01-05T00:00:00Z',
})

const threePeople = () => [person(11, 'Alice', 1), person(12, 'Bob', 2), person(13, 'Carol', 3)]

const empty = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
const pageOf = (content) => ({ ...empty, content, totalElements: content.length, totalPages: 1 })

const invitation = {
  id: 9,
  email: 'new@test.com',
  fullName: 'New Person',
  role: 'MEMBER',
  churchId: 1,
  churchName: 'Victory Church',
  roomId: 5,
  roomName: 'Youth',
  status: 'PENDING',
  expiresAt: '2030-01-01T00:00:00Z',
  createdAt: '2026-11-24T00:00:00Z',
  inviteUrl: null,
}

const names = (wrapper) =>
  wrapper.findAll('ol[aria-label="Participation order"] [data-member-name]').map((p) => p.text())
const byLabel = (wrapper, label) => wrapper.get(`[aria-label="${label}"]`)
const button = (wrapper, pattern) => wrapper.findAll('button').find((b) => pattern.test(b.text()))

// Mounted inside a RouterView so the route-leave guard has a real route record to attach to.
async function open() {
  const Blank = { render: () => null }
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/leader/rooms', name: 'leader-rooms', component: Blank },
      {
        path: '/leader/rooms/:roomId(\\d+)',
        name: 'leader-room',
        component: RoomDetailView,
        props: (route) => ({ roomId: Number(route.params.roomId) }),
      },
    ],
  })
  await router.push('/leader/rooms/5')
  const wrapper = mount(defineComponent({ render: () => h(RouterView) }), { global: { plugins: [router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('RoomDetailView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    roomsApi.getRoom.mockResolvedValue(room())
    api.getRoster.mockResolvedValue(threePeople())
    api.listRoomInvitations.mockResolvedValue(empty)
    api.listAvailableMembers.mockResolvedValue(empty)
  })

  // ---------- showing the room ----------

  it('shows "Church – Room", the leader and the members in order', async () => {
    const { wrapper } = await open()

    expect(wrapper.get('h1').text()).toBe('Victory Church – Youth')
    expect(wrapper.text()).toContain('Led by Leader Amy')
    expect(wrapper.text()).toContain('3 members')
    expect(names(wrapper)).toEqual(['Alice', 'Bob', 'Carol'])
    expect(wrapper.text()).toContain('alice@test.com')
  })

  it('invites the leader to add people when the room is empty', async () => {
    api.getRoster.mockResolvedValue([])

    const { wrapper } = await open()

    expect(wrapper.text()).toContain('Add someone from your church or invite a new person.')
  })

  it('says so when the room cannot be found', async () => {
    roomsApi.getRoom.mockRejectedValue(new ApiError(404, { detail: 'Room not found.' }))

    const { wrapper } = await open()

    expect(wrapper.text()).toContain('This room could not be found.')
  })

  it('hides every control and explains why when the room is archived', async () => {
    roomsApi.getRoom.mockResolvedValue(room({ status: 'ARCHIVED', archivedAt: '2026-02-01T00:00:00Z' }))

    const { wrapper } = await open()

    expect(wrapper.text()).toContain('This room is archived')
    expect(button(wrapper, /^Add from church$/)).toBeUndefined()
    expect(wrapper.find('[aria-label="Move Alice up"]').exists()).toBe(false)
    expect(button(wrapper, /^Remove\b/)).toBeUndefined()
    expect(names(wrapper)).toEqual(['Alice', 'Bob', 'Carol'])
  })

  // ---------- reordering ----------

  it('moves a member on screen without saving, and shows that the order is unsaved', async () => {
    const { wrapper } = await open()

    await byLabel(wrapper, 'Move Alice down').trigger('click')

    expect(names(wrapper)).toEqual(['Bob', 'Alice', 'Carol'])
    expect(api.saveOrder).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('The new order is not saved yet.')
  })

  it('disables the arrows that cannot be used', async () => {
    const { wrapper } = await open()

    expect(byLabel(wrapper, 'Move Alice up').attributes('disabled')).toBeDefined()
    expect(byLabel(wrapper, 'Move Carol down').attributes('disabled')).toBeDefined()
    expect(byLabel(wrapper, 'Move Bob up').attributes('disabled')).toBeUndefined()
    expect(byLabel(wrapper, 'Move Bob down').attributes('disabled')).toBeUndefined()
  })

  it('saves the new order', async () => {
    api.saveOrder.mockResolvedValue([person(12, 'Bob', 1), person(11, 'Alice', 2), person(13, 'Carol', 3)])
    const { wrapper } = await open()

    await byLabel(wrapper, 'Move Alice down').trigger('click')
    await button(wrapper, /^Save order$/).trigger('click')
    await flushPromises()

    expect(api.saveOrder).toHaveBeenCalledWith(5, [12, 11, 13])
    expect(wrapper.text()).toContain('Participation order saved.')
    expect(wrapper.text()).not.toContain('not saved yet')
    expect(names(wrapper)).toEqual(['Bob', 'Alice', 'Carol'])
  })

  it('discards the changes', async () => {
    const { wrapper } = await open()

    await byLabel(wrapper, 'Move Carol up').trigger('click')
    expect(names(wrapper)).toEqual(['Alice', 'Carol', 'Bob'])
    await button(wrapper, /^Discard changes$/).trigger('click')

    expect(names(wrapper)).toEqual(['Alice', 'Bob', 'Carol'])
    expect(api.saveOrder).not.toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('not saved yet')
  })

  it('refreshes the list when the room changed while the leader was reordering', async () => {
    api.getRoster
      .mockResolvedValueOnce(threePeople())
      .mockResolvedValueOnce([...threePeople(), person(14, 'Dan', 4)])
    api.saveOrder.mockRejectedValue(
      new ApiError(422, { detail: 'The list must contain every current member of the room exactly once.' }),
    )
    const { wrapper } = await open()

    await byLabel(wrapper, 'Move Alice down').trigger('click')
    await button(wrapper, /^Save order$/).trigger('click')
    await flushPromises()

    expect(names(wrapper)).toEqual(['Alice', 'Bob', 'Carol', 'Dan'])
    expect(wrapper.text()).toContain('The room changed while you were reordering')
    expect(wrapper.text()).not.toContain('not saved yet')
  })

  it('blocks adding and removing while the order is unsaved', async () => {
    const { wrapper } = await open()

    await byLabel(wrapper, 'Move Alice down').trigger('click')

    expect(button(wrapper, /^Add from church$/).attributes('disabled')).toBeDefined()
    expect(button(wrapper, /^Remove Alice/).attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Save or discard your order changes to add or remove members.')
  })

  it('asks before leaving with unsaved changes, and not otherwise', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false)
    const { wrapper, router } = await open()

    await byLabel(wrapper, 'Move Alice down').trigger('click')
    await router.push('/leader/rooms')
    expect(router.currentRoute.value.name).toBe('leader-room') // stayed
    expect(confirm).toHaveBeenCalledTimes(1)

    confirm.mockReturnValue(true)
    await router.push('/leader/rooms')
    expect(router.currentRoute.value.name).toBe('leader-rooms') // left

    confirm.mockRestore()
  })

  it('does not ask anything when nothing was changed', async () => {
    const confirm = vi.spyOn(window, 'confirm')
    const { router } = await open()

    await router.push('/leader/rooms')

    expect(router.currentRoute.value.name).toBe('leader-rooms')
    expect(confirm).not.toHaveBeenCalled()
    confirm.mockRestore()
  })

  // ---------- removing ----------

  it('removes a member after confirmation and refreshes the list', async () => {
    api.getRoster
      .mockResolvedValueOnce(threePeople())
      .mockResolvedValueOnce([person(12, 'Bob', 1), person(13, 'Carol', 2)])
    api.removeMember.mockResolvedValue(null)
    const { wrapper } = await open()

    await button(wrapper, /^Remove Alice/).trigger('click')
    expect(api.removeMember).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Remove Alice from this room?')

    await wrapper.get('dialog button.bg-navy-900').trigger('click')
    await flushPromises()

    expect(api.removeMember).toHaveBeenCalledWith(5, 11)
    expect(wrapper.text()).toContain('Alice was removed from the room.')
    expect(names(wrapper)).toEqual(['Bob', 'Carol'])
  })

  // ---------- adding from the church ----------

  it('adds an existing member and refreshes the order', async () => {
    api.getRoster
      .mockResolvedValueOnce(threePeople())
      .mockResolvedValueOnce([...threePeople(), person(21, 'Dan', 4)])
    api.listAvailableMembers.mockResolvedValue(
      pageOf([{ id: 21, fullName: 'Dan', email: 'dan@test.com' }]),
    )
    api.addMember.mockResolvedValue(person(21, 'Dan', 4))
    const { wrapper } = await open()

    await button(wrapper, /^Add from church$/).trigger('click')
    await flushPromises()
    expect(wrapper.get('dialog[open]').text()).toContain('Dan')

    await button(wrapper, /^Add Dan/).trigger('click')
    await flushPromises()

    expect(api.addMember).toHaveBeenCalledWith(5, 21)
    expect(wrapper.get('dialog[open]').text()).toContain('Dan was added as No. 4.')
    expect(names(wrapper)).toEqual(['Alice', 'Bob', 'Carol', 'Dan'])
  })

  it('explains when nobody is left to add', async () => {
    const { wrapper } = await open()

    await button(wrapper, /^Add from church$/).trigger('click')
    await flushPromises()

    expect(wrapper.get('dialog[open]').text()).toContain('Everyone from your church is already in this room.')
  })

  // ---------- inviting a new person ----------

  async function openInviteAndSubmit(wrapper, name, email) {
    await button(wrapper, /^Invite new person$/).trigger('click')
    await wrapper.get('dialog input[type="text"]').setValue(name)
    await wrapper.get('dialog input[type="email"]').setValue(email)
    await wrapper.get('dialog form').trigger('submit')
    await flushPromises()
  }

  it('shows the invitation link once, and refreshes the invitations when closed', async () => {
    const url = 'http://localhost:5173/accept-invitation#token=SECRET'
    api.inviteMember.mockResolvedValue({ ...invitation, inviteUrl: url })
    const { wrapper } = await open()

    await openInviteAndSubmit(wrapper, '  New Person ', ' new@test.com ')

    expect(api.inviteMember).toHaveBeenCalledWith(5, { email: 'new@test.com', fullName: 'New Person' })
    expect(wrapper.get('#invite-link').element.value).toBe(url)

    const before = api.listRoomInvitations.mock.calls.length
    await button(wrapper, /^Done$/).trigger('click')
    await flushPromises()

    expect(api.listRoomInvitations.mock.calls.length).toBe(before + 1)
    expect(wrapper.find('#invite-link').exists()).toBe(false)
    expect(wrapper.text()).toContain('Invitation created for new@test.com.')
  })

  it('does not call the server when the invitation form is incomplete', async () => {
    const { wrapper } = await open()

    await button(wrapper, /^Invite new person$/).trigger('click')
    await wrapper.get('dialog form').trigger('submit')
    await flushPromises()

    expect(api.inviteMember).not.toHaveBeenCalled()
    expect(wrapper.get('dialog[open]').text()).toContain('Please enter the person’s name.')
    expect(wrapper.get('dialog[open]').text()).toContain('Please enter an email address.')
  })

  it('shows the server message under the email when the person already has an account', async () => {
    api.inviteMember.mockRejectedValue(
      new ApiError(409, {
        detail: 'This person already has an account. Add them from the list of church members.',
      }),
    )
    const { wrapper } = await open()

    await openInviteAndSubmit(wrapper, 'Alice', 'alice@test.com')

    expect(wrapper.get('dialog[open]').text()).toContain('Add them from the list of church members.')
  })

  // ---------- invitations ----------

  it('lists the room’s invitations and revokes a pending one after confirmation', async () => {
    api.listRoomInvitations.mockResolvedValue(pageOf([invitation]))
    api.revokeRoomInvitation.mockResolvedValue(null)
    const { wrapper } = await open()

    expect(wrapper.text()).toContain('new@test.com')
    expect(wrapper.text()).toContain('Pending')

    await button(wrapper, /^Revoke\b/).trigger('click')
    await wrapper.get('dialog button.bg-navy-900').trigger('click')
    await flushPromises()

    expect(api.revokeRoomInvitation).toHaveBeenCalledWith(5, 9)
  })
})
