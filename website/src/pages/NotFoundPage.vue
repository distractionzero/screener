<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { useSeo } from '@/composables/useSeo'

useSeo({
  title: 'Page not found | Screener - Spam Call Blocker',
  description: 'The page you are looking for does not exist.',
  path: '/404',
  noindex: true,
})
</script>

<template>
  <section class="mx-auto max-w-3xl px-4 py-24 text-center sm:px-6">
    <p class="text-sm font-medium text-primary">404</p>
    <h1 class="mt-2 text-4xl font-semibold tracking-tight">Page not found</h1>
    <p class="mt-4 text-muted-foreground">The page you are looking for does not exist.</p>
    <Button as-child class="mt-8">
      <RouterLink to="/">Back to home</RouterLink>
    </Button>
  </section>
</template>
