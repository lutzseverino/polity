package com.odonta.polity.result;

import com.odonta.polity.model.PowerCode;
import com.odonta.polity.model.PowerHolderScope;

public record ConstitutionPowerChangeResult(
    PowerCode powerCode, PowerHolderScope holderScope, String holderOfficeCode) {}
