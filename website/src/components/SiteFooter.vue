<script setup lang="ts">
import { Separator } from '@/components/ui/separator'
import { site } from '@/lib/site'
</script>

<template>
  <footer class="border-t">
    <div class="mx-auto max-w-5xl px-4 py-8 text-sm text-muted-foreground sm:px-6">
      <nav aria-label="Footer" class="flex flex-wrap gap-x-6 gap-y-2">
        <RouterLink to="/privacy-policy" class="hover:text-foreground">Privacy policy</RouterLink>
        <RouterLink to="/open-source-licenses" class="hover:text-foreground">Open source licenses</RouterLink>
        <a :href="site.repo" target="_blank" rel="noopener noreferrer" class="hover:text-foreground">Source code</a>
      </nav>
      <Separator class="my-6" />
      <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <p>&copy; 2026 {{ site.name }}</p>
        <p>
          Crafted by
          <a
            :href="site.author.url"
            target="_blank"
            rel="author noopener noreferrer"
            class="font-medium text-foreground underline-offset-4 hover:underline">
            {{ site.author.name }}
          </a>
        </p>
      </div>
    </div>
  </footer>
</template>
