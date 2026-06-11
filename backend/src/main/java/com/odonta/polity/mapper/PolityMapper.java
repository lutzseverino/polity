package com.odonta.polity.mapper;

import com.odonta.polity.api.model.MemberResponse;
import com.odonta.polity.api.model.MembersResponse;
import com.odonta.polity.api.model.PolitiesResponse;
import com.odonta.polity.api.model.PolityResponse;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.PolityDetails;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PolityMapper {
  public PolityResponse toResponse(PolityDetails details) {
    return new PolityResponse(
        details.polity().getId(),
        details.polity().getName(),
        details.constitution().getVersion(),
        details.jurisdiction().getName(),
        details.institution().getName(),
        details.polity().getCreatedAt());
  }

  public PolitiesResponse toPolitiesResponse(List<PolityDetails> details) {
    return new PolitiesResponse(details.stream().map(this::toResponse).toList());
  }

  public MemberResponse toResponse(Membership member) {
    return new MemberResponse(
        member.getId(),
        member.getUserId(),
        member.getDisplayName(),
        member.getEmail(),
        MemberResponse.StatusEnum.ACTIVE,
        member.getAdmittedAt());
  }

  public MembersResponse toMembersResponse(List<Membership> members) {
    return new MembersResponse(members.stream().map(this::toResponse).toList());
  }
}
