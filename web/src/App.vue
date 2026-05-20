<template>
  <router-view v-slot="{ Component }">
    <transition name="slide-left" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
  <Toast />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import Toast from '@/components/auth/Toast.vue'
import { useUserStore } from '@/stores/user'
import { claimGuestHistoryIfPending } from '@/composables/useGuestAuth'

const userStore = useUserStore()

onMounted(() => {
  if (userStore.isLoggedIn && localStorage.getItem('guest_token')) {
    claimGuestHistoryIfPending()
  }
})
</script>
