export const site = {
  name: 'Screener - Spam Call Blocker',
  url: 'https://screener.codingcoffee.dev',
  repo: 'https://github.com/distractionzero/phone-spam-screener',
  license: 'https://www.gnu.org/licenses/gpl-3.0.html',
  ogImage: 'https://screener.codingcoffee.dev/og-image.png',
  author: { name: 'Ameya Shenoy', url: 'https://www.codingcoffee.dev' },
}

export const absoluteUrl = (path: string) => new URL(path, site.url).href

export const authorLd = {
  '@type': 'Person',
  '@id': `${site.url}/#author`,
  name: site.author.name,
  url: site.author.url,
}

export const breadcrumbLd = (name: string, path: string) => ({
  '@context': 'https://schema.org',
  '@type': 'BreadcrumbList',
  itemListElement: [
    { '@type': 'ListItem', position: 1, name: site.name, item: absoluteUrl('/') },
    { '@type': 'ListItem', position: 2, name, item: absoluteUrl(path) },
  ],
})
