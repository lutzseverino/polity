package com.odonta.polity.mapper;

import com.odonta.polity.model.MembershipInvitationResult;
import com.odonta.polity.repository.MembershipInvitationProjection;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface InvitationApplicationMapper {

  MembershipInvitationResult toResult(MembershipInvitationProjection projection);

  List<MembershipInvitationResult> toResults(List<MembershipInvitationProjection> projections);
}
