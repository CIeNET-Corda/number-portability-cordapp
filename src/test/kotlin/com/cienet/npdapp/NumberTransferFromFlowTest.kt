package com.cienet.npdapp

import com.cienet.npdapp.number.NumberAccessFlow
import com.cienet.npdapp.number.NumberContract
import com.cienet.npdapp.number.NumberState
import com.cienet.npdapp.number.NumberTransferFromFlow

import io.kotlintest.Description
import io.kotlintest.TestResult
import io.kotlintest.extensions.TestListener
import io.kotlintest.specs.StringSpec

import kotlin.test.assertEquals

import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode

class NumberTransferFromFlowTest : StringSpec() {

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
            listOf(a, b, c).forEach {
                it.registerInitiatedFlow(NumberAccessFlow.Acceptor::class.java)
                it.registerInitiatedFlow(NumberTransferFromFlow.Acceptor::class.java)
            }
            network.runNetwork()
        }

        override fun afterTest(description: Description, result: TestResult) {
            network.stopNodes()
        }
    })

    private fun addAANNumberState(number: String) {
        val numberState = NumberState(
                number,
                a.info.legalIdentities.first(),
                a.info.legalIdentities.first(),
                listOf(c.info.legalIdentities.first(), b.info.legalIdentities.first()))
        val transactionBuilder = TransactionBuilder(network.defaultNotaryIdentity)
        transactionBuilder
                .addOutputState(numberState, NumberContract.NUMBER_CONTRACT_ID, network.defaultNotaryIdentity)
                .addCommand(NumberContract.Commands.Access(), numberState.participants.map{it.owningKey})
        a.transaction{transactionBuilder.verify(a.services)}

        val signedTransactionA = a.services.signInitialTransaction(transactionBuilder)
        val signedTransactionB = b.services.addSignature(signedTransactionA)
        val signedTransactionC = c.services.addSignature(signedTransactionB)

        a.services.recordTransactions(signedTransactionC)
        b.services.recordTransactions(signedTransactionC)
        c.services.recordTransactions(signedTransactionC)
    }

    private fun addAAN2ABANumberState(number: String) {
        addAANNumberState(number)

        val flow = NumberTransferFromFlow.Initiator(number, a.info.legalIdentities.first())
        val future = b.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()
        // We check the recorded transaction in both vaults.
        for (node in listOf(a, b, c)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)
            val txOutputs = recordedTx!!.tx.outputs
            assert(txOutputs.size == 1)
            val recordedState = txOutputs.first().data as NumberState
            assertEquals(recordedState.number, number)
            assertEquals(recordedState.origOperator, a.info.singleIdentity())
            assertEquals(recordedState.currOperator, b.info.singleIdentity())
            assertEquals(recordedState.lastOperator, a.info.singleIdentity())
        }
    }

    private fun addABA2ACBNumberState(number: String) {
        addAAN2ABANumberState(number)

        val flow = NumberTransferFromFlow.Initiator(number, b.info.legalIdentities.first())
        val future = c.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        // We check the recorded transaction in both vaults.
        for (node in listOf(a, b, c)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)
            val txOutputs = recordedTx!!.tx.outputs
            assert(txOutputs.size == 1)
            val recordedState = txOutputs.first().data as NumberState
            assertEquals(recordedState.number, number)
            assertEquals(recordedState.origOperator, a.info.singleIdentity())
            assertEquals(recordedState.currOperator, c.info.singleIdentity())
            assertEquals(recordedState.lastOperator, b.info.singleIdentity())
        }
    }

    private fun addACB2AACNumberState(number: String) {
        addABA2ACBNumberState(number)
        val flow = NumberTransferFromFlow.Initiator(number, c.info.legalIdentities.first())
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        // We check the recorded transaction in both vaults.
        for (node in listOf(a, b, c)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)
            val txOutputs = recordedTx!!.tx.outputs
            assert(txOutputs.size == 1)
            val recordedState = txOutputs.first().data as NumberState
            assertEquals(recordedState.number, number)
            assertEquals(recordedState.origOperator, a.info.singleIdentity())
            assertEquals(recordedState.currOperator, a.info.singleIdentity())
            assertEquals(recordedState.lastOperator, c.info.singleIdentity())
        }
    }

    init {
        "NumberTransferFromFlow, the last operator is null" {
            addAAN2ABANumberState("18600123401")
        }
        "NumberTransferFromFlow, the last operator is origin" {
            addABA2ACBNumberState("18600123402")
        }
        "NumberTransferFromFlow, return to the origin operator" {
            addACB2AACNumberState("18600123403")
        }
    }
}