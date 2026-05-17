package com.example.backend.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {

	public static <T> PageResponse<T> fromPage(Page<?> page, List<T> mappedContent) {
		return new PageResponse<>(
				mappedContent,
				page.getTotalElements(),
				page.getTotalPages(),
				page.getNumber(),
				page.getSize());
	}
}
