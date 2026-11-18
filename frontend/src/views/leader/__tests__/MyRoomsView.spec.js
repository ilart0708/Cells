import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import MyRoomsView from '../MyRoomsView.vue'
import { ApiError } from '../../../api/http'
import * as api from '../../../api/rooms'

vi.mock('../../../api/rooms', () => ({
  listRooms: vi.fn(),
  createRoom: vi.fn(),
  renameRoom: vi.fn(),
  setRoomStatus: vi.fn(),
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
  id: 1,
  name: 'Youth Cell Group',
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

const pageOf = (content) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: content.length ? 1 : 0,
})

// Matches the visible label at the start, so "Archive" does not match the "Archived" filter.
const button = (wrapper, pattern) =>
  wrapper.findAll('button').find((b) => pattern.test(b.text().replace(/\s+/g, ' ').trim()))

async function mountView() {
  const wrapper = mount(MyRoomsView, {
    global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } },
  })
  await flushPromises()
  return wrapper
}

async function openCreateAndSubmit(wrapper, name) {
  await button(wrapper, /^Create room$/).trigger('click')
  await wrapper.get('dialog input').setValue(name)
  await wrapper.get('dialog form').trigger('submit')
  await flushPromises()
}

describe('MyRoomsView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    api.listRooms.mockResolvedValue(pageOf([room()]))
  })

  it('lists the rooms as "Church – Room" with the leader, member count and status', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Victory Church – Youth Cell Group')
    expect(wrapper.text()).toContain('Led by Leader Amy')
    expect(wrapper.text()).toContain('3 members')
    expect(wrapper.text()).toContain('Active')
  })

  it('words the member count for none and for one', async () => {
    api.listRooms.mockResolvedValue(
      pageOf([room({ id: 1, memberCount: 0 }), room({ id: 2, name: 'Second', memberCount: 1 })]),
    )

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('No members yet')
    expect(wrapper.text()).toContain('1 member ')
  })

  it('invites the leader to create the first room when there are none', async () => {
    api.listRooms.mockResolvedValue(pageOf([]))

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('You have no rooms yet')
  })

  it('asks for active rooms by default', async () => {
    await mountView()

    expect(api.listRooms).toHaveBeenCalledWith({ q: '', page: 0, size: 20, status: 'ACTIVE' })
  })

  it('reloads from the first page when the filter changes', async () => {
    api.listRooms.mockResolvedValue(pageOf([]))
    const wrapper = await mountView()

    await button(wrapper, /^Archived$/).trigger('click')
    await flushPromises()

    expect(api.listRooms).toHaveBeenLastCalledWith({ q: '', page: 0, size: 20, status: 'ARCHIVED' })
    expect(wrapper.text()).toContain('No archived rooms.')
  })

  it('shows an error with a retry when loading fails', async () => {
    api.listRooms.mockReset()
    api.listRooms
      .mockRejectedValueOnce(new Error('Cannot reach the server.'))
      .mockResolvedValueOnce(pageOf([room()]))

    const wrapper = await mountView()
    expect(wrapper.text()).toContain('Cannot reach the server.')

    await button(wrapper, /^Try again$/).trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Youth Cell Group')
  })

  // ---------- create ----------

  it('creates a room with a trimmed name and refreshes the list', async () => {
    api.listRooms.mockResolvedValue(pageOf([]))
    api.createRoom.mockResolvedValue(room())
    const wrapper = await mountView()

    await openCreateAndSubmit(wrapper, '  Youth  ')

    expect(api.createRoom).toHaveBeenCalledWith('Youth')
    expect(api.listRooms).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Room created.')
  })

  it('does not call the server for a blank name', async () => {
    const wrapper = await mountView()

    await openCreateAndSubmit(wrapper, '   ')

    expect(api.createRoom).not.toHaveBeenCalled()
    expect(wrapper.get('dialog[open]').text()).toContain('Please enter the room name.')
  })

  it('shows the server message when the name is already used', async () => {
    api.createRoom.mockRejectedValue(new ApiError(409, { detail: 'You already have a room with this name.' }))
    const wrapper = await mountView()

    await openCreateAndSubmit(wrapper, 'Youth Cell Group')

    expect(wrapper.get('dialog[open]').text()).toContain('You already have a room with this name.')
  })

  it('shows a general error when the leader has no church', async () => {
    api.createRoom.mockRejectedValue(
      new ApiError(422, { detail: 'Your account is not linked to a church.' }),
    )
    const wrapper = await mountView()

    await openCreateAndSubmit(wrapper, 'Youth')

    expect(wrapper.get('dialog[open]').text()).toContain('Your account is not linked to a church.')
  })

  // ---------- rename ----------

  it('renames a room, starting from its current name', async () => {
    api.renameRoom.mockResolvedValue(room())
    const wrapper = await mountView()

    await button(wrapper, /^Rename\b/).trigger('click')
    expect(wrapper.get('dialog input').element.value).toBe('Youth Cell Group')
    await wrapper.get('dialog input').setValue('Youth Fellowship')
    await wrapper.get('dialog form').trigger('submit')
    await flushPromises()

    expect(api.renameRoom).toHaveBeenCalledWith(1, 'Youth Fellowship')
    expect(wrapper.text()).toContain('Room renamed.')
  })

  it('offers only Restore, never Rename, for an archived room', async () => {
    api.listRooms.mockResolvedValue(
      pageOf([room({ status: 'ARCHIVED', archivedAt: '2026-02-01T00:00:00Z' })]),
    )

    const wrapper = await mountView()

    expect(button(wrapper, /^Rename\b/)).toBeUndefined()
    expect(button(wrapper, /^Restore\b/)).toBeDefined()
    expect(wrapper.text()).toContain('Archived')
  })

  // ---------- archive / restore ----------

  it('asks for confirmation, then archives the room', async () => {
    api.setRoomStatus.mockResolvedValue({})
    const wrapper = await mountView()

    await button(wrapper, /^Archive\b/).trigger('click')
    expect(api.setRoomStatus).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Archive Youth Cell Group?')

    await wrapper.get('dialog button.bg-navy-900').trigger('click')
    await flushPromises()

    expect(api.setRoomStatus).toHaveBeenCalledWith(1, 'ARCHIVED')
    expect(wrapper.text()).toContain('Youth Cell Group was archived.')
  })

  it('restores an archived room', async () => {
    api.listRooms.mockResolvedValue(
      pageOf([room({ status: 'ARCHIVED', archivedAt: '2026-02-01T00:00:00Z' })]),
    )
    api.setRoomStatus.mockResolvedValue({})
    const wrapper = await mountView()

    await button(wrapper, /^Restore\b/).trigger('click')
    await wrapper.get('dialog button.bg-navy-900').trigger('click')
    await flushPromises()

    expect(api.setRoomStatus).toHaveBeenCalledWith(1, 'ACTIVE')
    expect(wrapper.text()).toContain('Youth Cell Group was restored.')
  })

  it('explains a restore that clashes with an active room of the same name', async () => {
    api.listRooms.mockResolvedValue(
      pageOf([room({ status: 'ARCHIVED', archivedAt: '2026-02-01T00:00:00Z' })]),
    )
    api.setRoomStatus.mockRejectedValue(
      new ApiError(409, {
        detail: 'Another active room already has this name. Rename that room first, then restore this one.',
      }),
    )
    const wrapper = await mountView()

    await button(wrapper, /^Restore\b/).trigger('click')
    await wrapper.get('dialog button.bg-navy-900').trigger('click')
    await flushPromises()

    expect(wrapper.get('dialog[open]').text()).toContain('Another active room already has this name.')
  })
})
