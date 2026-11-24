<script setup>
import { RouterView, useRoute } from 'vue-router'

import AppShell from '@/layouts/AppShell.vue'
import PublicLayout from '@/layouts/PublicLayout.vue'

const route = useRoute()
</script>

<template>
  <PublicLayout v-if="route.meta.publicLayout" />
  <AppShell v-else />
</template>

