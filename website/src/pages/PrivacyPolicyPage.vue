<script setup lang="ts">
import { useSeo } from '@/composables/useSeo'
import { absoluteUrl, authorLd, breadcrumbLd } from '@/lib/site'

const path = '/privacy-policy'
const title = 'Privacy Policy | Phone Screener, the Offline Call Blocker'
const description =
  'Phone Screener does not collect, send or share any of your data. Learn what the call blocker sees, what it stores on your phone, and how you stay in control.'

const sections = [
  {
    heading: 'Summary',
    body: 'Phone Screener does not collect, send, or share any of your data. Everything it handles stays on your phone.',
  },
  {
    heading: 'What the app sees',
    body: 'When a call arrives from a number that isn’t in your contacts, Android passes the app the phone number, the caller name provided by your network (if any), and technical details about the call. The app checks these against your rules on the device to decide whether to reject the call. Calls from your contacts and calls you make are not recorded.',
  },
  {
    heading: 'What the app stores',
    body: 'A log of screened incoming calls, your rules, and your settings. These are kept in the app’s private storage on this device and are excluded from cloud backups and device-to-device transfers.',
  },
  {
    heading: 'No internet access',
    body: 'The app does not request the internet permission, so it cannot send anything off your device. It has no analytics, advertising, crash reporting, or third-party tracking.',
  },
  {
    heading: 'Permissions',
    body: 'The app only asks to be your caller ID & spam app (the call screening role). You can revoke it at any time in system settings.',
  },
  {
    heading: 'Your control',
    body: 'You can clear the call log at any time in Settings. Uninstalling the app deletes all of its data.',
  },
  {
    heading: 'This website',
    body: 'This website does not use cookies, analytics, or third-party scripts.',
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
      name: 'Privacy policy',
      url: absoluteUrl(path),
      description,
      dateModified: '2026-10-20',
      inLanguage: 'en-IN',
      author: authorLd,
    },
    breadcrumbLd('Privacy policy', path),
  ],
})
</script>

<template>
  <article class="mx-auto max-w-3xl px-4 py-16 sm:px-6 md:py-24">
    <header>
      <h1 class="text-4xl font-semibold tracking-tight">Privacy policy</h1>
      <p class="mt-3 text-sm text-muted-foreground">
        Last updated <time datetime="2026-10-20">20 October 2026</time>
      </p>
    </header>
    <section v-for="section in sections" :key="section.heading" class="mt-10">
      <h2 class="text-lg font-semibold">{{ section.heading }}</h2>
      <p class="mt-2 leading-relaxed text-muted-foreground">{{ section.body }}</p>
    </section>
  </article>
</template>
