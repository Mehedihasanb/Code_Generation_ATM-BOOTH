import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// GitHub Pages project site; keep "/" for local `npm run dev`
const pagesBase = process.env.GITHUB_ACTIONS === 'true' ? '/Code_Generation_ATM-BOOTH/' : '/';

export default defineConfig({
	base: pagesBase,
	plugins: [vue()],
	resolve: {
		alias: {
			'@': fileURLToPath(new URL('./src', import.meta.url)),
		},
	},
	server: {
		host: 'localhost',
		port: 5173,
		proxy: {
			'/api': {
				target: 'http://localhost:8080',
				changeOrigin: true,
			},
			'/auth': {
				target: 'http://localhost:8080',
				changeOrigin: true,
			},
			'/users': {
				target: 'http://localhost:8080',
				changeOrigin: true,
			},
			'/accounts': {
				target: 'http://localhost:8080',
				changeOrigin: true,
			},
			'/transactions': { target: 'http://localhost:8080', changeOrigin: true },
			'/atm': { target: 'http://localhost:8080', changeOrigin: true },
		},
	},
});
