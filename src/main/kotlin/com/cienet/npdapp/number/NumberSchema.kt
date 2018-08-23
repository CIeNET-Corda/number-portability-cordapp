package com.cienet.npdapp.number

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for NumberSchema.
 */
object NumberSchema

/**
 * An IOUState schema.
 */
object NumberSchemaV1 : MappedSchema(
        schemaFamily = NumberSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentNumber::class.java)) {
    @Entity
    @Table(name = "number_states")
    class PersistentNumber(
            @Column(name = "number")
            var number: String,

            @Column(name = "origOperator")
            var origOperatorName: String,

            @Column(name = "lastOperator")
            var lastOperatorName: String,

            @Column(name = "currOperator")
            var currOperatorName: String,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "", "", UUID.randomUUID())
    }
}