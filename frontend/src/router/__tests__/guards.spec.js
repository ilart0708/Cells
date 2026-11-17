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


    it('lets a logged-out visitor open an invitation link and keeps the fragment', async () => {
    await router.push('/accept-invitation#token=abc')

    expect(router.currentRoute.value.name).toBe('accept-invitation')
    expect(router.currentRoute.value.hash).toBe('#token=abc')
  })

  it('also lets a logged-in user open an invitation link', async () => {
    signIn(auth, 'ADMIN')

    await router.push('/accept-invitation#token=abc')

    expect(router.currentRoute.value.name).toBe('accept-invitation')
  })

    it('lets the administrator open the nested admin pages', async () => {
    signIn(auth, 'ADMIN')

    for (const path of ['/admin/churches', '/admin/pastors', '/admin/leaders']) {
      await router.push(path)
      expect(router.currentRoute.value.path).toBe(path)
    }
  })

  it('keeps other roles out of the nested admin pages', async () => {
    signIn(auth, 'PASTOR')

    await router.push('/admin/churches')

    expect(router.currentRoute.value.path).toBe('/pastor')
  })

    it('lets a pastor open the nested pastor pages', async () => {
    signIn(auth, 'PASTOR')

    await router.push('/pastor/leaders')

    expect(router.currentRoute.value.path).toBe('/pastor/leaders')
  })

  it('keeps other roles out of the nested pastor pages', async () => {
    signIn(auth, 'LEADER')

    await router.push('/pastor/leaders')

    expect(router.currentRoute.value.path).toBe('/leader')
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
