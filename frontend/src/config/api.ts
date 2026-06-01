/**
 * API base URL for production (GitHub Pages → Render). Empty in dev → Vite proxy handles paths.
 */
const configuredBase = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') ?? '';

export function apiUrl(path: string): string {
	if (path.startsWith('http://') || path.startsWith('https://')) {
		return path;
	}
	const normalizedPath = path.startsWith('/') ? path : `/${path}`;
	return `${configuredBase}${normalizedPath}`;
}
