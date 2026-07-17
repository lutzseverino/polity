package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CreateMembershipInvitationRequest;
import com.odonta.polity.api.model.MembershipInvitationCompletionResponse;
import com.odonta.polity.api.model.MembershipInvitationResponse;
import com.odonta.polity.api.model.MembershipInvitationTokenResponse;
import com.odonta.polity.input.CreateMembershipInvitationInput;
import com.odonta.polity.result.MembershipInvitationCompletionResult;
import com.odonta.polity.result.MembershipInvitationResult;
import com.odonta.polity.result.MembershipInvitationTokenResult;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface MembershipInvitationTransportMapper {

  CreateMembershipInvitationInput toInput(CreateMembershipInvitationRequest request);

  MembershipInvitationResponse toResponse(MembershipInvitationResult result);

  MembershipInvitationTokenResponse toResponse(MembershipInvitationTokenResult result);

  MembershipInvitationCompletionResponse toResponse(MembershipInvitationCompletionResult result);

  List<MembershipInvitationResponse> toResponses(List<MembershipInvitationResult> results);
}
