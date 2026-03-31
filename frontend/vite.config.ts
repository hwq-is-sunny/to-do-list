import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  // Required for Electron file:// loading in packaged builds.
  base: './',
  plugins: [react()],
})
