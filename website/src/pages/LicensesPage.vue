<script setup lang="ts">
import { ExternalLink } from '@lucide/vue'
import { useSeo } from '@/composables/useSeo'
import { absoluteUrl, breadcrumbLd, site } from '@/lib/site'

const path = '/open-source-licenses'
const title = 'Open Source Licenses | Phone Screener Android Call Blocker'
const description =
  'Phone Screener is free software under the GPLv3, built on open source libraries from Google and JetBrains. See each library the Android app uses and its license.'

const ANDROIDX = 'https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt'

const libraries = [
  { name: 'AndroidX Activity, Core, Lifecycle', owner: 'Google', license: 'Apache License 2.0', url: ANDROIDX },
  { name: 'AndroidX Navigation', owner: 'Google', license: 'Apache License 2.0', url: ANDROIDX },
  { name: 'AndroidX DataStore', owner: 'Google', license: 'Apache License 2.0', url: ANDROIDX },
  {
    name: 'Protocol Buffers (bundled in DataStore)',
    owner: 'Google',
    license: 'BSD 3-Clause License',
    url: 'https://github.com/protocolbuffers/protobuf/blob/main/LICENSE',
  },
  { name: 'Jetpack Compose UI, Foundation, Material 3', owner: 'Google', license: 'Apache License 2.0', url: ANDROIDX },
  {
    name: 'Material Icons and Material Symbols',
    owner: 'Google',
    license: 'Apache License 2.0',
    url: 'https://github.com/google/material-design-icons/blob/master/LICENSE',
  },
  {
    name: 'Kotlin Standard Library',
    owner: 'JetBrains',
    license: 'Apache License 2.0',
    url: 'https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt',
  },
  {
    name: 'kotlinx.coroutines',
    owner: 'JetBrains',
    license: 'Apache License 2.0',
    url: 'https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt',
  },
]

useSeo({
  title,
  description,
  path,
  jsonLd: [
    {
      '@context': 'https://schema.org',
      '@type': 'WebPage',
      name: 'Open source licenses',
      url: absoluteUrl(path),
      description,
      inLanguage: 'en-IN',
    },
    breadcrumbLd('Open source licenses', path),
  ],
})
</script>

<template>
  <article class="mx-auto max-w-3xl px-4 py-16 sm:px-6 md:py-24">
    <header>
      <h1 class="text-4xl font-semibold tracking-tight">Open source licenses</h1>
      <p class="mt-4 leading-relaxed text-muted-foreground">
        {{ site.name }} is free software, licensed under the
        <a :href="site.license" target="_blank" rel="noopener noreferrer" class="font-medium text-foreground underline underline-offset-4">
          GNU General Public License v3
        </a>. The Android app is built on these open source libraries.
      </p>
    </header>

    <section aria-labelledby="libraries-title" class="mt-10">
      <h2 id="libraries-title" class="sr-only">Libraries</h2>
      <ul class="divide-y rounded-lg border">
        <li v-for="lib in libraries" :key="lib.name" class="flex items-center justify-between gap-4 px-5 py-4">
          <div class="min-w-0">
            <p class="font-medium">{{ lib.name }}</p>
            <p class="mt-0.5 text-sm text-muted-foreground">{{ lib.owner }}</p>
          </div>
          <a
            :href="lib.url"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex shrink-0 items-center gap-1.5 text-sm text-primary underline-offset-4 hover:underline"
            :aria-label="`${lib.license} for ${lib.name}`"
          >
            {{ lib.license }}
            <ExternalLink class="size-3.5" aria-hidden="true" />
          </a>
        </li>
      </ul>
      <p class="mt-4 text-sm text-muted-foreground">
        Also includes other AndroidX libraries these depend on, all under the Apache License 2.0.
      </p>
    </section>
  </article>
</template>
