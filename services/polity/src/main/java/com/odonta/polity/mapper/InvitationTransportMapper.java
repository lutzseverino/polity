package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CreateMemberInvitationRequest;
import com.odonta.polity.api.model.MemberInvitationResponse;
import com.odonta.polity.model.CreateMemberInvitationInput;
import com.odonta.polity.model.MembershipInvitationResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface InvitationTransportMapper {

  CreateMemberInvitationInput toInput(CreateMemberInvitationRequest request);

  MemberInvitationResponse toResponse(MembershipInvitationResult result);

  List<MemberInvitationResponse> toResponses(List<MembershipInvitationResult> results);
}
