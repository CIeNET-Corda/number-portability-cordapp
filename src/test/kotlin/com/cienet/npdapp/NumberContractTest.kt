package com.cienet.npdapp

import com.cienet.npdapp.number.NumberContract
import com.cienet.npdapp.number.NumberState
import com.cienet.npdapp.number.NumberContract.Companion.NUMBER_CONTRACT_ID
import io.kotlintest.specs.StringSpec

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger

class NumberContractTest : StringSpec({

    val ledgerServices = MockServices()
    val cmcc = TestIdentity(CordaX500Name("CMCC", "China", "CN"))
    val cucc = TestIdentity(CordaX500Name("CUCC", "China", "CN"))
    val ctcc = TestIdentity(CordaX500Name("CTCC", "China", "CN"))

    "Access transaction" {
        ledgerServices.ledger {
            transaction {
                output(NUMBER_CONTRACT_ID, NumberState("18600575799", cucc.party, cucc.party, null))
                command(listOf(cucc.publicKey, cmcc.publicKey, ctcc.publicKey), NumberContract.Commands.Access())
                verifies()
            }
        }
    }
    "Access transaction: The format of Number must be valid" {
        ledgerServices.ledger {
            transaction {
                output(NUMBER_CONTRACT_ID, NumberState("110", cucc.party, cucc.party, null))
                command(listOf(cucc.publicKey, cucc.publicKey), NumberContract.Commands.Access())
                `fails with`("The format of Number must be valid, contract")
            }
        }
    }
    "Access transaction: The Origin and Current Operator must be same" {
        ledgerServices.ledger {
            transaction {
                output(NUMBER_CONTRACT_ID, NumberState("18600575799", cucc.party, cmcc.party, null))
                command(listOf(cucc.publicKey, cmcc.publicKey), NumberContract.Commands.Access())
                `fails with`("The Origin and Current Operator must be same")
            }
        }
    }
    "Access transaction: The Last Operator must be blank" {
    ledgerServices.ledger {
        transaction {
            output(NUMBER_CONTRACT_ID, NumberState("18600575799", cucc.party, cucc.party, ctcc.party))
            command(listOf(cucc.publicKey, cucc.publicKey, ctcc.publicKey), NumberContract.Commands.Access())
            `fails with`("The Last Operator must be blank")
        }
    }
}
})
