try {
  var mode = localStorage.getItem('vueuse-color-scheme')
  if (mode === 'dark' || (mode !== 'light' && matchMedia('(prefers-color-scheme: dark)').matches))
    document.documentElement.classList.add('dark')
} catch (e) {}
