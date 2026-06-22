package com.odonta.polity.mapper;

import com.odonta.polity.model.MembershipResult;
import com.odonta.polity.repository.MembershipProjection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PolityMapperConfig.class)
public interface MembershipApplicationMapper {

  @Mapping(target = "name", source = "displayName")
  MembershipResult toResult(MembershipProjection projection);

  List<MembershipResult> toResults(List<MembershipProjection> projections);
}
