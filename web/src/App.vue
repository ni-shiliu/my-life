<template>
  <router-view v-slot="{ Component }">
    <transition name="slide-left" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
  <Toast />
</template>

<script setup lang="ts">
import Toast from '@/components/auth/Toast.vue'
</script>
