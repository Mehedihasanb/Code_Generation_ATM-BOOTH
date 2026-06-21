type ApiErrorBody = {
	message?: string;
	fieldErrors?: Record<string, string>;
};

export function parseApiError(body: string, fallback: string): {
	message: string;
	fieldErrors?: Record<string, string>;
} {
	if (!body.trim()) {
		return { message: fallback };
	}

	try {
		const parsed = JSON.parse(body) as ApiErrorBody;
		const message = parsed.message?.trim() || fallback;
		const fieldErrors = parsed.fieldErrors && Object.keys(parsed.fieldErrors).length > 0
			? parsed.fieldErrors
			: undefined;
		return { message, fieldErrors };
	} catch {
		return { message: body.trim() };
	}
}

export function parseApiErrorMessage(body: string, fallback: string): string {
	return parseApiError(body, fallback).message;
}
