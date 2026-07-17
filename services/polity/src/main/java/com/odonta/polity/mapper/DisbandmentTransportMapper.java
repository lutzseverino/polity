package com.odonta.polity.mapper;

import com.odonta.polity.api.model.CreateDisbandmentMotionRequest;
import com.odonta.polity.input.CreateDisbandmentMotionInput;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface DisbandmentTransportMapper {
  CreateDisbandmentMotionInput toInput(CreateDisbandmentMotionRequest request);
}
