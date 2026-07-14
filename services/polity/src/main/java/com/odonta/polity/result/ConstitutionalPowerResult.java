package com.odonta.polity.result;

import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;

public record ConstitutionalPowerResult(
    PowerCode code,
    String name,
    String nameKey,
    PowerHolderScope holderScope,
    String holderOfficeCode) {}
