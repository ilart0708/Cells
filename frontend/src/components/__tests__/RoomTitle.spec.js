import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import RoomTitle from '../RoomTitle.vue'

const props = { churchName: 'Victory Church', roomName: 'Youth Cell Group', leaderName: 'Leader Amy' }

describe('RoomTitle', () => {
  it('shows "Church – Room" with the leader as the subtitle', () => {
    const wrapper = mount(RoomTitle, { props })

    expect(wrapper.get('h3').text()).toBe('Victory Church – Youth Cell Group')
    expect(wrapper.text()).toContain('Led by Leader Amy')
  })

  it('uses the requested heading level', () => {
    const wrapper = mount(RoomTitle, { props: { ...props, level: 1 } })

    expect(wrapper.find('h1').exists()).toBe(true)
    expect(wrapper.find('h3').exists()).toBe(false)
  })
})
