package com.cienet.npdapp

import com.cienet.npdapp.number.NumberAccessFlow
import com.cienet.npdapp.number.NumberContract
import com.cienet.npdapp.number.NumberState
import com.cienet.npdapp.number.NumberTransferToFlow

import io.kotlintest.Description
import io.kotlintest.TestResult
import io.kotlintest.extensions.TestListener
import io.kotlintest.specs.StringSpec
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowException
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
            listOf(a, b, c).forEach {
                it.registerInitiatedFlow(NumberAccessFlow.Acceptor::class.java)
                it.registerInitiatedFlow(NumberTransferToFlow.Acceptor::class.java)
            }
            network.runNetwork()
        }

        override fun afterTest(description: Description, result: TestResult) {
            network.stopNodes()
        }
    })

    fun addAANNumberState(number: String): StateAndRef<NumberState> {
        val numberState = NumberState(
                number,
                a.info.legalIdentities[0],
                a.info.legalIdentities[0],
                null)
        val transactionBuilder = TransactionBuilder(network.defaultNotaryIdentity)
        transactionBuilder
                .addOutputState(numberState, NumberContract.NUMBER_CONTRACT_ID, network.defaultNotaryIdentity)
                .addCommand(NumberContract.Commands.Access(), a.info.legalIdentities[0].owningKey)
        a.transaction{transactionBuilder.verify(a.services)}

        val partSignedTransaction = a.services.signInitialTransaction(transactionBuilder)
        val signedTransaction = a.services.addSignature(partSignedTransaction)

        a.services.recordTransactions(signedTransaction)

        val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results = a.services.vaultService.queryBy(NumberState::class.java, criteria)
        val startAndRef = results.states.stream()
                .filter{it.state.data.number == number && it.state.data.currOperator == a.info.singleIdentity()}
                .findAny()
                .orElse(null) ?: throw FlowException("Can not find this number belongs to myself.")
        return startAndRef
    }

    init {
        "NumberTransferToFlow, the last operator is null" {
            val numberState = NumberState("18600123401", a.info.legalIdentities[0], a.info.legalIdentities[0], null)
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

            val flow = NumberTransferToFlow.Initiator("18600123401", b.info.legalIdentities[0])
            val future = a.startFlow(flow)
            network.runNetwork()

            val signedTx = future.getOrThrow()
            // We check the recorded transaction in both vaults.
            for (node in listOf(a, b)) {
                val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)
                val txOutputs = recordedTx!!.tx.outputs
                assert(txOutputs.size == 1)
                val recordedState = txOutputs[0].data as NumberState
                assertEquals(recordedState.number, "18600123401")
                assertEquals(recordedState.origOperator, a.info.singleIdentity())
                assertEquals(recordedState.currOperator, b.info.singleIdentity())
                assertEquals(recordedState.lastOperator, a.info.singleIdentity())
            }
        }
        "NumberTransferToFlow, the last operator is origin" {
            val numberState = NumberState(
                    "18600123402",
                    a.info.legalIdentities[0],
                    b.info.legalIdentities[0],
                    a.info.legalIdentities[0])
            val transactionBuilder = TransactionBuilder(network.defaultNotaryIdentity)
            transactionBuilder
                    .addInputState(addAANNumberState("18600123402"))
                    .addOutputState(
                            numberState,
                            NumberContract.NUMBER_CONTRACT_ID,
                            network.defaultNotaryIdentity)
                    .addCommand(
                            NumberContract.Commands.TransferTo(),
                            a.info.legalIdentities[0].owningKey,
                            b.info.legalIdentities[0].owningKey)

            b.transaction{transactionBuilder.verify(b.services)}

            val partSignedTransaction = b.services.signInitialTransaction(transactionBuilder)
            val signedTransaction = a.services.addSignature(partSignedTransaction)

            //a.services.recordTransactions(signedTransaction)
            b.services.recordTransactions(signedTransaction)

            val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
            val results = b.services.vaultService.queryBy(NumberState::class.java, criteria)
            val startAndRef = results.states.stream()
                    .filter{it.state.data.number == "18600123402" && it.state.data.currOperator == b.info.legalIdentities.first()}
                    .findAny()
                    .orElse(null) ?: throw FlowException("Can not find this number belongs to myself.")


            val flow = NumberTransferToFlow.Initiator("18600123402", c.info.legalIdentities[0])
            val future = b.startFlow(flow)
            network.runNetwork()

            val signedTx = future.getOrThrow()
            // We check the recorded transaction in both vaults.
            for (node in listOf(a, b, c)) {
                val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)
                val txOutputs = recordedTx!!.tx.outputs
                assert(txOutputs.size == 1)
                val recordedState = txOutputs[0].data as NumberState
                assertEquals(recordedState.number, "18600123402")
                assertEquals(recordedState.origOperator, a.info.singleIdentity())
                assertEquals(recordedState.currOperator, c.info.singleIdentity())
                assertEquals(recordedState.lastOperator, b.info.singleIdentity())
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