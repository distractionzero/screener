<script setup lang="ts">
import AppLogo from '@/components/AppLogo.vue'
import GithubIcon from '@/components/GithubIcon.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { Button } from '@/components/ui/button'
import { site } from '@/lib/site'

const links = [
  { label: 'Features', hash: '#features' },
  { label: 'How it works', hash: '#how-it-works' },
  { label: 'FAQ', hash: '#faq' },
]
</script>

<template>
  <header class="sticky top-0 z-40 border-b bg-background/85 backdrop-blur supports-[backdrop-filter]:bg-background/70">
    <div class="mx-auto flex h-16 max-w-5xl items-center justify-between gap-4 px-4 sm:px-6">
      <RouterLink to="/" class="flex items-center gap-2.5 font-semibold tracking-tight" aria-label="Screener home">
        <AppLogo class="size-8" />
        <span>{{ site.name }}</span>
      </RouterLink>
      <nav aria-label="Main" class="flex items-center gap-1">
        <RouterLink
          v-for="link in links"
          :key="link.hash"
          :to="{ path: '/', hash: link.hash }"
          class="hidden rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:text-foreground md:inline-block"
        >
          {{ link.label }}
        </RouterLink>
        <ThemeToggle class="ml-1" />
        <Button as-child variant="outline" size="sm" class="ml-1">
          <a :href="site.repo" target="_blank" rel="noopener noreferrer">
            <GithubIcon class="size-4" />
            <span>GitHub</span>
          </a>
        </Button>
      </nav>
    </div>
  </header>
</template>
