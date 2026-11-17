import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ChurchManagementView from '../ChurchManagementView.vue'
import { ApiError } from '../../../api/http'
import * as churchesApi from '../../../api/churches'

vi.mock('../../../api/churches', () => ({
  listChurches: vi.fn(),
  createChurch: vi.fn(),
  renameChurch: vi.fn(),
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

const page = (names) => ({
  content: names.map((name, i) => ({ id: i + 1, name, status: 'ACTIVE', createdAt: '2026-01-05T00:00:00Z' })),
  page: 0,
  size: 20,
  totalElements: names.length,
  totalPages: 1,
})

const buttonWithText = (wrapper, text) => wrapper.findAll('button').find((b) => b.text().includes(text))

async function mountView() {
  const wrapper = mount(ChurchManagementView)
  await flushPromises()
  return wrapper
}

async function openAndSubmit(wrapper, name) {
  await buttonWithText(wrapper, 'Add church').trigger('click')
  await wrapper.get('dialog input').setValue(name)
  await wrapper.get('dialog form').trigger('submit')
  await flushPromises()
}

describe('ChurchManagementView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('lists the churches', async () => {
    churchesApi.listChurches.mockResolvedValue(page(['Victory Church', 'Grace Community Church']))

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Victory Church')
    expect(wrapper.text()).toContain('Grace Community Church')
  })

  it('invites the admin to add the first church when there are none', async () => {
    churchesApi.listChurches.mockResolvedValue(page([]))

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('No churches yet')
  })

  it('shows an error with a retry when loading fails', async () => {
    churchesApi.listChurches.mockRejectedValueOnce(new Error('Cannot reach the server.'))
    churchesApi.listChurches.mockResolvedValueOnce(page(['Victory Church']))

    const wrapper = await mountView()
    expect(wrapper.text()).toContain('Cannot reach the server.')

    await buttonWithText(wrapper, 'Try again').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Victory Church')
  })

  it('creates a church with a trimmed name and refreshes the list', async () => {
    churchesApi.listChurches.mockResolvedValue(page([]))
    churchesApi.createChurch.mockResolvedValue({ id: 9 })
    const wrapper = await mountView()

    await openAndSubmit(wrapper, '  Hope Chapel ')

    expect(churchesApi.createChurch).toHaveBeenCalledWith('Hope Chapel')
    expect(churchesApi.listChurches).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Church created.')
  })

  it('does not call the server for a blank name', async () => {
    churchesApi.listChurches.mockResolvedValue(page([]))
    const wrapper = await mountView()

    await openAndSubmit(wrapper, '   ')

    expect(churchesApi.createChurch).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Please enter the church name.')
  })

  it('shows the server message when the name already exists', async () => {
    churchesApi.listChurches.mockResolvedValue(page([]))
    churchesApi.createChurch.mockRejectedValue(
      new ApiError(409, { detail: 'A church with this name already exists.' }),
    )
    const wrapper = await mountView()

    await openAndSubmit(wrapper, 'Victory Church')

    expect(wrapper.get('dialog').text()).toContain('A church with this name already exists.')
  })

  it('renames a church', async () => {
    churchesApi.listChurches.mockResolvedValue(page(['Victory Church']))
    churchesApi.renameChurch.mockResolvedValue({ id: 1 })
    const wrapper = await mountView()

    await buttonWithText(wrapper, 'Rename').trigger('click')
    expect(wrapper.get('dialog input').element.value).toBe('Victory Church')
    await wrapper.get('dialog input').setValue('Victory Church Main')
    await wrapper.get('dialog form').trigger('submit')
    await flushPromises()

    expect(churchesApi.renameChurch).toHaveBeenCalledWith(1, 'Victory Church Main')
    expect(wrapper.text()).toContain('Church renamed.')
  })
})
