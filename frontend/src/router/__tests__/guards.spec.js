import { beforeEach, describe, expect, it, vi } from 'vitest'

const AREAS = {
  ADMIN: '/admin',
  PASTOR: '/pastor',
  LEADER: '/leader',
  MEMBER: '/member',
}

function signIn(auth, role) {
  auth.accessToken = 'test-token'
  auth.user = {
    id: 1,
    email: `${role.toLowerCase()}@test.com`,
    fullName: `Test ${role}`,
    role,
  }
}

describe('router guards', () => {
  let router
  let auth

  // A fresh router and store per test: a router that already sits on /login would
  // ignore a second push to /login as a "duplicate navigation" and never run the guard.
  beforeEach(async () => {
    vi.resetModules()
    const { createPinia, setActivePinia } = await import('pinia')
    setActivePinia(createPinia())
    router = (await import('../index')).default
    auth = (await import('../../stores/auth')).useAuthStore()
  })

  it('sends a logged-out visitor to login and remembers the target', async () => {
    await router.push('/admin')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/admin')
  })

  it('sends unknown paths to login when logged out', async () => {
    await router.push('/does-not-exist')

    expect(router.currentRoute.value.name).toBe('login')
  })

  it('lets a logged-out visitor open the login page', async () => {
    await router.push('/login')

    expect(router.currentRoute.value.name).toBe('login')
  })

  for (const [role, path] of Object.entries(AREAS)) {
    it(`${role} reaches ${path}`, async () => {
      signIn(auth, role)

      await router.push(path)

      expect(router.currentRoute.value.path).toBe(path)
    })

    it(`${role} opening /login lands in ${path}`, async () => {
      signIn(auth, role)

      await router.push('/login')

      expect(router.currentRoute.value.path).toBe(path)
    })

    it(`${role} opening / lands in ${path}`, async () => {
      signIn(auth, role)

      await router.push('/')

      expect(router.currentRoute.value.path).toBe(path)
    })

    for (const [otherRole, otherPath] of Object.entries(AREAS)) {
      if (otherRole === role) continue
      it(`${role} is bounced away from ${otherPath}`, async () => {
        signIn(auth, role)

        await router.push(otherPath)

        expect(router.currentRoute.value.path).toBe(path)
      })
    }
  }
})
