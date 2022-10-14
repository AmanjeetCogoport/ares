package com.cogoport.ares.api.migration.repository

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import org.mapstruct.factory.Mappers

@Factory
class MapperConfig {
    @Bean
    fun convertModelToEntity(): newMapper {
        return Mappers.getMapper(newMapper::class.java)
    }
}
