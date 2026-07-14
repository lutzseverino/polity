package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CreateMemberInvitationRequest;
import com.odonta.polity.api.model.MemberInvitationResponse;
import com.odonta.polity.input.CreateMemberInvitationInput;
import com.odonta.polity.result.MembershipInvitationResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface MembershipInvitationTransportMapper {

  CreateMemberInvitationInput toInput(CreateMemberInvitationRequest request);

  MemberInvitationResponse toResponse(MembershipInvitationResult result);

  List<MemberInvitationResponse> toResponses(List<MembershipInvitationResult> results);
}
