<script setup lang="ts">
import { Moon, Sun } from '@lucide/vue'
import { useDark, useToggle } from '@vueuse/core'
import { Button } from '@/components/ui/button'

const toggle = useToggle(useDark())
</script>

<template>
  <Button variant="ghost" size="icon" aria-label="Toggle dark mode" @click="toggle()">
    <Sun class="size-4 dark:hidden" aria-hidden="true" />
    <Moon class="hidden size-4 dark:block" aria-hidden="true" />
  </Button>
</template>
