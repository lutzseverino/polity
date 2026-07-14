package com.odonta.polity.resolver;

import com.odonta.polity.model.OfficeElectionTallyResult;
import com.odonta.polity.result.OfficeElectionResult;

record OfficeElectionResolution(OfficeElectionResult result, OfficeElectionTallyResult tally) {}
