import { defineConfig } from "vite";

export default defineConfig({
  server: {
    host: "0.0.0.0",
    port: 4173,
  },
  build: {
    outDir: "dist",
    emptyOutDir: true,
    rollupOptions: {
      input: {
        main: "./index.html",
        overview: "./overview.html",
        incidents: "./incidents.html",
        blacklist: "./blacklist.html",
        drivers: "./drivers.html",
        reports: "./reports.html",
        analytics: "./analytics.html",
        driver: "./driver.html",
      },
    },
  },
});
