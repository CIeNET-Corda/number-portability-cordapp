package com.cienet.npdapp.number

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

/**
 * A implementation of a smart contract in Corda for Number Portability.
 *
 * This contract enforces rules regarding the creation of a valid [NumberState], which in turn encapsulates an [Number].
 *
 * For a new [Number] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [Number].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
open class NumberContract : Contract {
    companion object {
        @JvmStatic
        val NUMBER_CONTRACT_ID = "com.cienet.npdapp.number.NumberContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        requireThat {
            "Only one command of Number Contract" using (tx.commands.size == 1)
            "Only one input state of Number Contract" using (tx.outputStates.size == 1)
        }

        val outputState = tx.outputsOfType<NumberState>().single()

        val accessCommands = tx.commands.select<Commands.Access>()
        if (accessCommands.isNotEmpty()) {
            //process Access
            requireThat {
                val numberRegex = Regex("^1(3|4|5|7|8)\\d{9}\$")
                "The format of Number must be valid" using numberRegex.matches(outputState.number)
                "The Origin and Current Operator must be same" using (outputState.currOperator == outputState.origOperator)
                "The Last Operator must be blank" using (outputState.lastOperator == null)
            }
        }
        val transferToCommands = tx.commands.select<Commands.TransferTo>()
        if (transferToCommands.isNotEmpty()) {
            // process TransferTo
            val inputState = tx.inputsOfType<NumberState>().single()
            requireThat {
                "Number in Input and Output must be same" using (inputState.number == outputState.number)
                "Origin Operator in Input and Output must be same" using (inputState.origOperator == outputState.origOperator)
                "Input's Current Operator and Output's Last Operator must be same" using (inputState.currOperator == outputState.lastOperator)
                "Output's Current Operator and Last Operator must NOT be same" using (outputState.currOperator != outputState.lastOperator)
            }
        }
    }

    /**
     * This contract only implements one command, Access, TransferTo.
     */
    interface Commands : CommandData {
        class Access : Commands
        class TransferTo : Commands
    }
}
