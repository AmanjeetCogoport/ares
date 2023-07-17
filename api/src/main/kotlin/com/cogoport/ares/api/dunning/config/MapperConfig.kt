package com.cogoport.ares.api.dunning.config

import com.cogoport.ares.api.dunning.mapper.DunningMapper
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import org.mapstruct.factory.Mappers

@Factory
class MapperConfig {

    @Bean
    fun getDunningMapper(): DunningMapper {
        return Mappers.getMapper(DunningMapper::class.java)
    }
}
