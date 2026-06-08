export function parseApiErrorMessage(body: string, fallback: string): string {
	if (!body.trim()) {
		return fallback;
	}

	try {
		const parsed = JSON.parse(body) as { message?: string };
		if (parsed.message && parsed.message.trim()) {
			return parsed.message.trim();
		}
	} catch {
		// not JSON, use raw text below
	}

	return body.trim();
}
