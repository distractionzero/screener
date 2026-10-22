<script setup lang="ts">
import { Lock, MapPin, PhoneIncoming, PhoneOff, ScrollText, SlidersHorizontal, UserCheck } from '@lucide/vue'
import GithubIcon from '@/components/GithubIcon.vue'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useSeo } from '@/composables/useSeo'
import { absoluteUrl, authorLd, site } from '@/lib/site'

const title = 'Phone Screener: Free Spam Call Blocker for Android in India'
const description =
  'Block spam and telemarketing calls on Android with your own rules, by number or caller name. A free, open source call blocker for India that works offline.'

const activity = [
  { caller: '+91 140 xxx xxxx', rule: 'Telemarketing (140 series)', blocked: true },
  { caller: 'Spam Alert', rule: 'Carrier spam label', blocked: true },
  { caller: '+91 160 xxx xxxx', rule: 'Service calls (160 series)', blocked: false },
  { caller: '+91 80 71xx xxxx', rule: 'Bangalore cloud telephony', blocked: true },
]

const features = [
  {
    icon: SlidersHorizontal,
    title: 'Rules you control',
    body: 'Block or allow calls with a regular expression on the phone number or on the caller name your network sends. Test a rule before you save it, or block all unknown callers.',
  },
  {
    icon: PhoneIncoming,
    title: 'Works with your phone app',
    body: 'Phone Screener does not replace your dialer. It only takes Android’s caller ID & spam role, so Google Phone or any other phone app keeps working as usual.',
  },
  {
    icon: UserCheck,
    title: 'Your contacts always ring',
    body: 'Android never sends calls from saved contacts to the screener, so friends, family and colleagues always get through.',
  },
  {
    icon: MapPin,
    title: 'Made for Indian networks',
    body: 'Ready-made rules for 140 series telemarketing, 160 series service numbers and cloud telephony ranges, plus a rule for spam labels sent by Jio, Airtel, Vi or BSNL.',
  },
  {
    icon: Lock,
    title: 'Private and offline',
    body: 'No internet permission, no analytics, no ads and no tracking. Your rules and call log stay on your phone and are excluded from backups.',
  },
  {
    icon: ScrollText,
    title: 'A log of every screened call',
    body: 'See each call the screener handled, the rule that matched, and the raw details your carrier passed along.',
  },
]

const defaultRules = [
  { name: 'Telemarketing (140 series)', matches: 'Numbers starting with 140', block: true },
  { name: 'Service calls (160 series)', matches: 'Numbers starting with 160 (banks, insurance, government)', block: false },
  { name: 'Bangalore cloud telephony', matches: 'Numbers starting with 8071', block: true },
  { name: 'Ahmedabad cloud telephony', matches: 'Numbers starting with 7935', block: true },
  { name: 'Ahmedabad cloud telephony', matches: 'Numbers starting with 7971', block: true },
  { name: 'Carrier spam label', matches: 'Caller name containing “spam”, “fraud” or “scam”', block: true },
]

const steps = [
  {
    title: 'Set it as your caller ID & spam app',
    body: 'Open Phone Screener once and grant the call screening role. You can revoke it any time in system settings.',
  },
  {
    title: 'An unknown number calls',
    body: 'Android hands Phone Screener the number and the caller name from your network. Saved contacts skip this step.',
  },
  {
    title: 'Your rules decide',
    body: 'Allow rules are checked first, then block rules. If nothing matches, the call rings, unless “Block all unknown callers” is on.',
  },
  {
    title: 'Every decision is logged',
    body: 'Rejected and allowed calls show up in Activity on your phone, along with the rule that matched.',
  },
]

const faqs = [
  {
    q: 'Does Phone Screener replace my phone app?',
    a: 'No. It only holds Android’s call screening role and works alongside your existing dialer, such as Google Phone.',
  },
  {
    q: 'Will it block calls from my contacts?',
    a: 'No. Android does not send calls from saved contacts to call screening apps, so your contacts always ring.',
  },
  {
    q: 'How do I block 140 series telemarketing calls?',
    a: 'Install Phone Screener and set it as your caller ID & spam app. The 140 series block rule is on by default, and the 160 series used by banks, insurance and government is allowed.',
  },
  {
    q: 'Does it send my call data anywhere?',
    a: 'No. The app does not request the internet permission, so it cannot send anything off your phone. It has no analytics, ads or crash reporting.',
  },
  {
    q: 'Can I write my own blocking rules?',
    a: 'Yes. Add allow or block rules with a regular expression on the phone number or on the caller name, and test them right in the rule editor.',
  },
  {
    q: 'Which Android versions are supported?',
    a: 'Android 10 and newer, which is when the call screening role became available to third-party apps.',
  },
  {
    q: 'Is Phone Screener free?',
    a: 'Yes. It is free and open source software, licensed under the GNU General Public License v3.',
  },
  {
    q: 'How do I install Phone Screener?',
    a: 'Phone Screener is distributed as source code. Build the APK by following the README on GitHub, then install it on your phone.',
  },
]

useSeo({
  title,
  description,
  path: '/',
  jsonLd: [
    {
      '@context': 'https://schema.org',
      '@graph': [
        authorLd,
        {
          '@type': 'WebSite',
          '@id': `${site.url}/#website`,
          name: site.name,
          url: absoluteUrl('/'),
          inLanguage: 'en-IN',
          publisher: { '@id': authorLd['@id'] },
        },
        {
          '@type': 'MobileApplication',
          '@id': `${site.url}/#app`,
          name: site.name,
          description,
          url: absoluteUrl('/'),
          image: site.ogImage,
          operatingSystem: 'Android 10+',
          applicationCategory: 'UtilitiesApplication',
          applicationSubCategory: 'Call blocker',
          countriesSupported: 'IN',
          softwareVersion: '1.0',
          isAccessibleForFree: true,
          license: site.license,
          offers: { '@type': 'Offer', price: '0', priceCurrency: 'INR' },
          featureList: features.map(f => f.title),
          author: { '@id': authorLd['@id'] },
        },
        {
          '@type': 'SoftwareSourceCode',
          name: `${site.name} source code`,
          codeRepository: site.repo,
          programmingLanguage: 'Kotlin',
          license: site.license,
          author: { '@id': authorLd['@id'] },
        },
      ],
    },
    {
      '@context': 'https://schema.org',
      '@type': 'FAQPage',
      mainEntity: faqs.map(({ q, a }) => ({
        '@type': 'Question',
        name: q,
        acceptedAnswer: { '@type': 'Answer', text: a },
      })),
    },
  ],
})
</script>

<template>
  <section aria-labelledby="hero-title" class="mx-auto max-w-5xl px-4 pb-20 pt-16 sm:px-6 md:pt-24">
    <div class="grid items-center gap-12 md:grid-cols-[1.15fr_1fr]">
      <div>
        <Badge variant="secondary" class="mb-5">Free &amp; open source · Android</Badge>
        <h1 id="hero-title" class="text-4xl font-semibold tracking-tight text-balance sm:text-5xl">
          Block spam and telemarketing calls on Android
        </h1>
        <p class="mt-5 text-lg text-pretty text-muted-foreground">
          Phone Screener is a free, open source call blocker for India that follows your rules. Block or allow calls
          by number or by the caller name your network sends, from a single pesky caller to an entire series.
        </p>
        <div class="mt-8 flex flex-wrap gap-3">
          <Button as-child size="lg">
            <a :href="site.repo" target="_blank" rel="noopener noreferrer">
              <GithubIcon class="size-4" />
              View source on GitHub
            </a>
          </Button>
          <Button as-child size="lg" variant="outline">
            <a href="#how-it-works">See how it works</a>
          </Button>
        </div>
        <ul class="mt-8 flex flex-wrap gap-x-5 gap-y-2 text-sm text-muted-foreground">
          <li>No internet permission</li>
          <li>No ads or tracking</li>
          <li>Android 10+</li>
        </ul>
      </div>

      <figure>
        <Card class="gap-0 py-0 shadow-sm">
          <CardContent class="p-0">
            <p class="border-b px-5 py-3 text-sm font-medium">Activity</p>
            <ul class="divide-y">
              <li v-for="item in activity" :key="item.caller" class="flex items-center gap-3 px-5 py-3.5">
                <span
                  class="grid size-9 shrink-0 place-items-center rounded-full"
                  :class="item.blocked ? 'bg-muted text-destructive' : 'bg-muted text-primary'"
                >
                  <PhoneOff v-if="item.blocked" class="size-4" aria-hidden="true" />
                  <PhoneIncoming v-else class="size-4" aria-hidden="true" />
                </span>
                <span class="min-w-0 flex-1">
                  <span class="block truncate text-sm font-medium">{{ item.caller }}</span>
                  <span class="block truncate text-xs text-muted-foreground">{{ item.rule }}</span>
                </span>
                <Badge :variant="item.blocked ? 'secondary' : 'outline'">
                  {{ item.blocked ? 'Blocked' : 'Allowed' }}
                </Badge>
              </li>
            </ul>
          </CardContent>
        </Card>
        <figcaption class="mt-3 text-center text-xs text-muted-foreground">
          Example of the call activity log in Phone Screener
        </figcaption>
      </figure>
    </div>
  </section>

  <section id="features" aria-labelledby="features-title" class="scroll-mt-20 border-t">
    <div class="mx-auto max-w-5xl px-4 py-20 sm:px-6">
      <h2 id="features-title" class="text-3xl font-semibold tracking-tight text-balance">
        A spam call blocker that stays out of your way
      </h2>
      <p class="mt-3 max-w-2xl text-muted-foreground">
        Phone Screener quietly checks calls from unknown numbers and rejects the ones that match your rules.
      </p>
      <ul class="mt-12 grid gap-x-10 gap-y-10 sm:grid-cols-2 lg:grid-cols-3">
        <li v-for="feature in features" :key="feature.title">
          <component :is="feature.icon" class="size-5 text-primary" aria-hidden="true" />
          <h3 class="mt-4 font-medium">{{ feature.title }}</h3>
          <p class="mt-2 text-sm leading-relaxed text-muted-foreground">{{ feature.body }}</p>
        </li>
      </ul>
    </div>
  </section>

  <section id="default-rules" aria-labelledby="rules-title" class="scroll-mt-20 border-t">
    <div class="mx-auto max-w-5xl px-4 py-20 sm:px-6">
      <h2 id="rules-title" class="text-3xl font-semibold tracking-tight text-balance">
        Built-in rules for Indian spam numbers
      </h2>
      <p class="mt-3 max-w-2xl text-muted-foreground">
        These rules are ready from the first launch. Switch any of them off, or add your own on top.
      </p>
      <div class="mt-10 overflow-x-auto rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead scope="col" class="px-4">Rule</TableHead>
              <TableHead scope="col" class="px-4">Matches</TableHead>
              <TableHead scope="col" class="px-4 text-right">Action</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="rule in defaultRules" :key="rule.name">
              <TableCell class="px-4 font-medium">{{ rule.name }}</TableCell>
              <TableCell class="px-4 text-muted-foreground">{{ rule.matches }}</TableCell>
              <TableCell class="px-4 text-right">
                <Badge :variant="rule.block ? 'secondary' : 'outline'">{{ rule.block ? 'Block' : 'Allow' }}</Badge>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
    </div>
  </section>

  <section id="how-it-works" aria-labelledby="how-title" class="scroll-mt-20 border-t">
    <div class="mx-auto max-w-5xl px-4 py-20 sm:px-6">
      <h2 id="how-title" class="text-3xl font-semibold tracking-tight text-balance">How Phone Screener works</h2>
      <ol class="mt-12 grid gap-10 sm:grid-cols-2 lg:grid-cols-4">
        <li v-for="(step, i) in steps" :key="step.title">
          <span class="text-sm font-medium text-primary">Step {{ i + 1 }}</span>
          <h3 class="mt-2 font-medium">{{ step.title }}</h3>
          <p class="mt-2 text-sm leading-relaxed text-muted-foreground">{{ step.body }}</p>
        </li>
      </ol>
    </div>
  </section>

  <section id="faq" aria-labelledby="faq-title" class="scroll-mt-20 border-t">
    <div class="mx-auto max-w-3xl px-4 py-20 sm:px-6">
      <h2 id="faq-title" class="text-3xl font-semibold tracking-tight">Frequently asked questions</h2>
      <Accordion type="single" collapsible :unmount-on-hide="false" class="mt-8">
        <AccordionItem v-for="(faq, i) in faqs" :key="faq.q" :value="`faq-${i}`">
          <AccordionTrigger class="text-base">{{ faq.q }}</AccordionTrigger>
          <AccordionContent class="text-muted-foreground">{{ faq.a }}</AccordionContent>
        </AccordionItem>
      </Accordion>
    </div>
  </section>

  <section aria-labelledby="cta-title" class="border-t">
    <div class="mx-auto max-w-5xl px-4 py-20 text-center sm:px-6">
      <h2 id="cta-title" class="text-3xl font-semibold tracking-tight text-balance">Take back your phone</h2>
      <p class="mx-auto mt-3 max-w-xl text-muted-foreground">
        Phone Screener is free software under the GPLv3. Read the code, build it yourself, and make it yours.
      </p>
      <Button as-child size="lg" class="mt-8">
        <a :href="site.repo" target="_blank" rel="noopener noreferrer">
          <GithubIcon class="size-4" />
          Get the source code
        </a>
      </Button>
    </div>
  </section>
</template>
