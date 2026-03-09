package gr.alexc.acodelearn.resource;

import gr.alexc.acodelearn.resource.ResourceType;

import java.time.Instant;

public record ResourceSummaryView(Long id, String name, ResourceType resourceType, Long courseId, Instant createdAt) {}
