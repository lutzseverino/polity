package com.odonta.polity.mapper;

import com.odonta.polity.result.GovernmentFormationResult;
import java.time.OffsetDateTime;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(config = PolityMapperConfig.class)
public interface GovernmentFormationApplicationMapper {
  @BeanMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
  GovernmentFormationResult toResult(
      boolean complete,
      OffsetDateTime completedAt,
      int minimumFullGovernmentMembers,
      long activeMemberCount,
      long standingMemberCount);
}
