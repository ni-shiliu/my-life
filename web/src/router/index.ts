import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/register/RegisterView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/home/HomeView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/agent/:id',
      name: 'AgentEdit',
      component: () => import('@/views/agent/AgentEditView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/chat/:agentId',
      name: 'Chat',
      component: () => import('@/views/chat/ChatView.vue'),
      meta: { requiresAuth: false }
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (!to.meta.requiresAuth && userStore.isLoggedIn && to.name !== 'Chat') {
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
