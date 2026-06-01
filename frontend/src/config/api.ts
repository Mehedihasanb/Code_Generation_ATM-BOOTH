/**
 * API base URL.
 * - Dev (`npm run dev`): empty → Vite proxy forwards /auth, /accounts, etc. to localhost:8080.
 * - Production build: VITE_API_BASE_URL if set, otherwise the deployed Render backend.
 */
const PRODUCTION_BACKEND_URL = 'https://code-generation-atm-booth.onrender.com';

const envBase = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '');
const configuredBase = envBase || (import.meta.env.PROD ? PRODUCTION_BACKEND_URL : '');

export function apiUrl(path: string): string {
	if (path.startsWith('http://') || path.startsWith('https://')) {
		return path;
	}
	const normalizedPath = path.startsWith('/') ? path : `/${path}`;
	return `${configuredBase}${normalizedPath}`;
}
