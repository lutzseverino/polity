package com.odonta.polity.mapper;

import com.odonta.polity.repository.MembershipInvitationProjection;
import com.odonta.polity.result.MembershipInvitationResult;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface MembershipInvitationApplicationMapper {

  @BeanMapping(ignoreUnmappedSourceProperties = "invitedBy")
  MembershipInvitationResult toResult(
      MembershipInvitationProjection projection, String polityName, String invitedByName);
}
