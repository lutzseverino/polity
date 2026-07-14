package com.odonta.polity.result;

import com.odonta.polity.model.JurisdictionKind;
import java.util.UUID;

public record JurisdictionResult(UUID id, String name, JurisdictionKind kind) {}
