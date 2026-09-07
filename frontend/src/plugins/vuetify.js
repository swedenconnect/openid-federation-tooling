/**
 * plugins/vuetify.js
 *
 * Framework documentation: https://vuetifyjs.com`
 */

// Styles
import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'

// Composables
import { createVuetify } from 'vuetify'

// https://vuetifyjs.com/en/introduction/why-vuetify/#feature-guides
// Colors match the Sweden Connect Sandbox design tokens
// (https://sandbox.swedenconnect.se/home/index.html).
export default createVuetify({
  theme: {
    defaultTheme: 'light',
    themes: {
      light: {
        colors: {
          primary: '#5a6751',
          secondary: '#cd7a6e',
          background: '#f3f3f3',
          surface: '#ffffff',
          error: '#cd7a6e',
        },
      },
    },
  },
})
