package com.cienet.npdapp

import com.cienet.npdapp.number.NumberAccessFlow
import com.cienet.npdapp.number.NumberContract
import com.cienet.npdapp.number.NumberState
import com.cienet.npdapp.number.NumberContract.Companion.NUMBER_CONTRACT_ID
import io.kotlintest.Description
import io.kotlintest.TestResult
import io.kotlintest.extensions.TestListener
import io.kotlintest.specs.StringSpec
import net.corda.core.contracts.TransactionVerificationException

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockServices
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.ledger
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith


class NumberAccessFlowTest : StringSpec() {

    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    override fun listeners(): List<TestListener> = listOf(object : TestListener {

        override fun beforeTest(description: Description) {
            network = MockNetwork(listOf("com.cienet.npdapp.number"))
            a = network.createPartyNode()
            b = network.createPartyNode()
            c = network.createPartyNode()
            // For real nodes this happens automatically, but we have to manually register the flow for tests.
            listOf(a, b, c).forEach { it.registerInitiatedFlow(NumberAccessFlow.Acceptor::class.java) }
            network.runNetwork()
        }

        override fun afterTest(description: Description, result: TestResult) {
            network.stopNodes()
        }
    })

    init {
        "NumberAccessFlow" {
            val flow = NumberAccessFlow.Initiator("18600123400")
            val future = a.startFlow(flow)
            network.runNetwork()

            val signedTx = future.getOrThrow()
            // We check the recorded transaction in both vaults.
            for (node in listOf(a)) {
                val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)
                val txOutputs = recordedTx!!.tx.outputs
                assert(txOutputs.size == 1)
                val recordedState = txOutputs[0].data as NumberState
                assertEquals(recordedState.number, "18600123400")
                assertEquals(recordedState.origOperator, a.info.singleIdentity())
                assertEquals(recordedState.currOperator, a.info.singleIdentity())
                assertEquals(recordedState.lastOperator, null)
            }
        }
        "NumberAccessFlow Exception: The format of Number must be invalid" {
            val flow = NumberAccessFlow.Initiator("10086")
            val future = a.startFlow(flow)
            network.runNetwork()

            //assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
            assertFailsWith<TransactionVerificationException> ("The format of Number must be invalid"){future.getOrThrow() }
        }
    }
}