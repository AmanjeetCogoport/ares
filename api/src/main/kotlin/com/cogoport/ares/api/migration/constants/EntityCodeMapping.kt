package com.cogoport.ares.api.migration.constants

import java.util.UUID

enum class EntityCodeMapping(val entityCode: Short, val entityCodeId: UUID) {
    CODE101(101, UUID.fromString("6fd98605-9d5d-479d-9fac-cf905d292b88")),
    CODE201(201, UUID.fromString("c7e1390d-ec41-477f-964b-55423ee84700")),
    CODE301(301, UUID.fromString("ee09645b-5f34-4d2e-8ec7-6ac83a7946e1")),
    CODE401(401, UUID.fromString("04bd1037-c110-4aad-8ecc-fc43e9d4069d"));

    companion object {

        fun getByEntityCode(entityCode: String): UUID {
            return EntityCodeMapping.valueOf("CODE$entityCode").entityCodeId
        }
        fun getEntityCodeByEntityCodeId(entityCodeId: UUID): Short {
            return EntityCodeMapping.valueOf(entityCodeId.toString()).entityCode
        }
    }
}
