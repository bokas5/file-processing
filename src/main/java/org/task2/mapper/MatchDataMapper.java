package org.task2.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.task2.jpa.MatchDataJpa;
import org.task2.model.MatchDataDTO;

@Mapper(componentModel = "cdi")
public interface MatchDataMapper {
    MatchDataMapper INSTANCE = Mappers.getMapper(MatchDataMapper.class);

    MatchDataJpa toEntity(MatchDataDTO dto);

    MatchDataDTO toDTO(MatchDataJpa entity);
}

