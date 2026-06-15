package com.odonta.polity.model;

import java.time.OffsetDateTime;

public record CertificationResult(boolean passed, String explanation, OffsetDateTime certifiedAt) {}
