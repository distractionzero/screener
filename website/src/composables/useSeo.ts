import { useHead } from '@unhead/vue'
import { absoluteUrl } from '@/lib/site'

interface SeoOptions {
  title: string
  description: string
  path: string
  jsonLd?: object[]
  noindex?: boolean
}

export function useSeo({ title, description, path, jsonLd = [], noindex = false }: SeoOptions) {
  const url = absoluteUrl(path)
  useHead({
    title,
    link: noindex ? [] : [{ rel: 'canonical', href: url }],
    meta: [
      { name: 'description', content: description },
      { name: 'robots', content: noindex ? 'noindex, follow' : 'index, follow, max-image-preview:large' },
      { property: 'og:title', content: title },
      { property: 'og:description', content: description },
      { property: 'og:url', content: url },
      { name: 'twitter:title', content: title },
      { name: 'twitter:description', content: description },
    ],
    script: jsonLd.map(data => ({ type: 'application/ld+json', innerHTML: JSON.stringify(data) })),
  })
}
