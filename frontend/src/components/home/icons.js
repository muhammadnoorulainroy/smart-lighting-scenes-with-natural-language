/**
 * @fileoverview SVG icon components for the home page features section.
 *
 * Provides render-function-based icon components using Vue's `h` function.
 * Each icon is an SVG with consistent styling (stroke-based, 24x24 viewBox).
 *
 * Also exports the `features` array which maps icons to feature descriptions
 * for the landing page feature grid.
 *
 * @module components/home/icons
 */

import { h } from 'vue'

/**
 * Creates an SVG icon component from path data.
 *
 * Returns a Vue component that renders an SVG element with
 * the provided path(s). Icons use stroke styling and are
 * sized via CSS (default viewBox is 24x24).
 *
 * @param {string[]} paths - SVG path `d` attributes
 * @returns {Object} Vue component with render function
 * @private
 */
const createIcon = paths => ({
  render: () =>
    h(
      'svg',
      { fill: 'none', stroke: 'currentColor', viewBox: '0 0 24 24' },
      paths.map(d =>
        h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d })
      )
    )
})

// Icon Components

/** Microphone icon for voice control feature */
export const MicrophoneIcon = createIcon([
  'M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z'
])

/** Lightning bolt icon for automation feature */
export const LightningIcon = createIcon(['M13 10V3L4 14h7v7l9-11h-7z'])

/** Mobile phone icon for app control feature */
export const MobileIcon = createIcon([
  'M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z'
])

/** Color palette icon for custom scenes feature */
export const PaletteIcon = createIcon([
  'M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01'
])

/** Shield icon for security feature */
export const ShieldIcon = createIcon([
  'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z'
])

/** Sliders icon for advanced controls feature */
export const SlidersIcon = createIcon([
  'M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4'
])

/** Arrow icon for call-to-action buttons */
export const ArrowIcon = createIcon(['M13 7l5 5m0 0l-5 5m5-5H6'])

/** Play button icon for demo/video buttons */
export const PlayIcon = createIcon([
  'M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z',
  'M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
])

/** Bolt icon (duplicate of Lightning for different contexts) */
export const BoltIcon = createIcon(['M13 10V3L4 14h7v7l9-11h-7z'])

/** Lightbulb icon for lighting-related UI */
export const LightbulbIcon = createIcon([
  'M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z'
])

/** Checkmark in circle icon for success states */
export const CheckCircleIcon = createIcon(['M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'])

// Feature Data

/**
 * Feature configurations for the landing page feature grid.
 *
 * Each feature includes:
 * - icon: Vue component to render
 * - title: Feature heading
 * - description: Feature explanation
 * - iconBgClass: Tailwind gradient classes for icon background
 * - hoverClass: Tailwind class for hover text color
 *
 * @type {Array<{icon: Object, title: string, description: string, iconBgClass: string, hoverClass: string}>}
 */
export const features = [
  {
    icon: MicrophoneIcon,
    title: 'Natural Language Control',
    description:
      'Control your lights naturally. Say "dim the lights for movie time" or "brighten the kitchen at sunset" and it just works.',
    iconBgClass: 'bg-gradient-to-br from-primary-400 to-primary-600',
    hoverClass: 'group-hover:text-primary-500'
  },
  {
    icon: LightningIcon,
    title: 'Smart Automation',
    description:
      'Create intelligent schedules that adapt to sunrise, sunset, and your daily routines automatically.',
    iconBgClass: 'bg-gradient-to-br from-accent-400 to-accent-600',
    hoverClass: 'group-hover:text-accent-500'
  },
  {
    icon: MobileIcon,
    title: 'Mobile Control',
    description:
      'Control your lights from anywhere with our beautiful and intuitive mobile app for Android.',
    iconBgClass: 'bg-gradient-to-br from-orange-400 to-orange-600',
    hoverClass: 'group-hover:text-orange-500'
  },
  {
    icon: PaletteIcon,
    title: 'Custom Scenes',
    description:
      'Create and save custom lighting scenes for any mood or activity - work, relax, party, or sleep.',
    iconBgClass: 'bg-gradient-to-br from-red-400 to-red-600',
    hoverClass: 'group-hover:text-red-500'
  },
  {
    icon: ShieldIcon,
    title: 'Secure & Private',
    description:
      'Your data is encrypted and secure. We respect your privacy and never sell your information.',
    iconBgClass: 'bg-gradient-to-br from-emerald-400 to-emerald-600',
    hoverClass: 'group-hover:text-emerald-500'
  },
  {
    icon: SlidersIcon,
    title: 'Advanced Controls',
    description:
      'Fine-tune brightness, color temperature, and RGB colors with precision controls for perfect lighting.',
    iconBgClass: 'bg-gradient-to-br from-cyan-400 to-cyan-600',
    hoverClass: 'group-hover:text-cyan-500'
  }
]
