package com.cienet.npdapp.number

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState


/**
  * The state object recording Number agreements between operators.
  *
  * A state must implement [ContractState] or one of its descendants.
  *
  * @param number the number.
  * @param origOperator the original operator of this number.
  * @param lastOperator the last operator of this number.
  * @param currOperator the current operator of this number.
  */
data class NumberState(val number: String,
                       val origOperator: Party,
                       val currOperator: Party,
                       val broadcastTo: List<Party>,
                       val lastOperator: Party? = null,
                       override val linearId: UniqueIdentifier = UniqueIdentifier()):
    LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List <Party> get () {
        var participants = listOf(origOperator, currOperator).plus(broadcastTo)
        if (lastOperator != null)
            participants = participants.plus(lastOperator)
        return participants.distinct()
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is NumberSchemaV1 -> NumberSchemaV1.PersistentNumber(
                    this.number,
                    this.origOperator.name.toString(),
                    this.lastOperator?.name?.toString() ?: "",
                    this.currOperator.name.toString(),
                    this.broadcastTo.toString(),
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(NumberSchemaV1)
}