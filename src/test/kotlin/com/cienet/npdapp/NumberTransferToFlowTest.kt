package com.cienet.npdapp

import com.cienet.npdapp.number.NumberAccessFlow
import com.cienet.npdapp.number.NumberContract
import com.cienet.npdapp.number.NumberState
import com.cienet.npdapp.number.NumberTransferToFlow

import io.kotlintest.Description
import io.kotlintest.TestResult
import io.kotlintest.extensions.TestListener
import io.kotlintest.specs.StringSpec
import net.corda.core.contracts.AttachmentResolutionException
import net.corda.core.contracts.TransactionResolutionException

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode

class NumberTransferToFlowTest : StringSpec() {

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
        "NumberTransferToFlow" {
            val numberState = NumberState("18600123400", a.info.legalIdentities[0], a.info.legalIdentities[0], null)
            val transactionBuilder = TransactionBuilder(network.defaultNotaryIdentity)
            transactionBuilder
                    .addOutputState(numberState, NumberContract.NUMBER_CONTRACT_ID, network.defaultNotaryIdentity)
                    .addCommand(NumberContract.Commands.Access(), a.info.legalIdentities[0].owningKey)

            a.transaction{
                try {
                    transactionBuilder.verify(a.services)
                } catch (e: TransactionVerificationException) {
                    assertEquals(1, 1)
                } catch (e: TransactionResolutionException) {
                    assertEquals(2, 2)
                } catch (e: AttachmentResolutionException) {
                    assertEquals(3, 3)
                }
                null
            }

            val partSignedTransaction = a.services.signInitialTransaction(transactionBuilder)
            val signedTransaction = a.services.addSignature(partSignedTransaction)

            a.services.recordTransactions(signedTransaction)

            val flow = NumberTransferToFlow.Initiator("18600123400", b.info.legalIdentities[0])
            val future = a.startFlow(flow)
            network.runNetwork()

            val signedTx = future.getOrThrow()
            // We check the recorded transaction in both vaults.
            for (node in listOf(a, b)) {
                val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)
                val txOutputs = recordedTx!!.tx.outputs
                assert(txOutputs.size == 1)
                val recordedState = txOutputs[0].data as NumberState
                assertEquals(recordedState.number, "18600123400")
                assertEquals(recordedState.origOperator, a.info.singleIdentity())
                assertEquals(recordedState.currOperator, b.info.singleIdentity())
                assertEquals(recordedState.lastOperator, a.info.singleIdentity())
            }
        }
//        "NumberAccessFlow Exception: The format of Number must be invalid" {
//            val flow = NumberAccessFlow.Initiator("10086")
//            val future = a.startFlow(flow)
//            network.runNetwork()
//
//            //assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
//            assertFailsWith<TransactionVerificationException> ("The format of Number must be invalid"){future.getOrThrow() }
//        }
    }
}