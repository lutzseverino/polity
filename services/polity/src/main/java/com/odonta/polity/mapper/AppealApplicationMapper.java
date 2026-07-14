package com.odonta.polity.mapper;

import com.odonta.polity.repository.AppealProjection;
import com.odonta.polity.result.AppealResult;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface AppealApplicationMapper {

  AppealResult toResult(AppealProjection projection, String appellantName);
}
