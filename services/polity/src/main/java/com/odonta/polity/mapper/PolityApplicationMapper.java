package com.odonta.polity.mapper;

import com.odonta.polity.model.MembershipResult;
import com.odonta.polity.model.PolityResult;
import com.odonta.polity.repository.MembershipProjection;
import com.odonta.polity.repository.PolityProjection;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface PolityApplicationMapper {

  PolityResult toResult(PolityProjection projection);

  List<PolityResult> toResults(List<PolityProjection> projections);

  MembershipResult toResult(MembershipProjection projection);

  List<MembershipResult> toMemberResults(List<MembershipProjection> projections);
}
