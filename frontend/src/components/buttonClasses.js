const base =
  'inline-flex min-h-11 items-center justify-center rounded-xl px-4 text-sm font-bold focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-60'

// Gold is for the one primary action per area, as the design guide asks.
export const btnPrimary = `${base} bg-gold-500 text-navy-900 hover:brightness-95 focus-visible:outline-navy-900`
export const btnSecondary = `${base} bg-navy-900 text-white hover:bg-navy-700 focus-visible:outline-gold-500`
export const btnOutline = `${base} border border-line-input bg-white text-navy-900 hover:bg-app-bg focus-visible:outline-navy-900`
