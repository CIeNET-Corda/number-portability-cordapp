package com.cienet.npdapp.number

import co.paralleluniverse.fibers.Suspendable
import com.cienet.npdapp.number.NumberContract.Companion.NUMBER_CONTRACT_ID
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.core.utilities.unwrap

/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the Number encapsulated
 * within an [NumberState].
 *
 * In our simple example, the [Acceptor] always accepts a valid Number.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
object NumberTransferToFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val number: String, private val otherParty: Party) : FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : Step("Generating transaction based on new Number.")
            object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
            object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            val me = serviceHub.myInfo.legalIdentities.first()

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            if (otherParty == me) {
                throw FlowException("Can not transfer a number to self.")
            }
            val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
            val results = serviceHub.vaultService.queryBy(NumberState::class.java, criteria)
            val orderStateRef = results.states.stream()
                    .filter{it.state.data.number == number && it.state.data.currOperator == me}
                    .findAny()
                    .orElse(null) ?: throw FlowException("Can not transfer a number to self.")

            val inputState = orderStateRef.state.data
            val outputState = NumberState(number, inputState.origOperator, otherParty, inputState.currOperator)
            val participants = outputState.participants.plus(inputState.participants).distinct()
            val txCommand = Command(NumberContract.Commands.TransferTo(), participants.map { it.owningKey })

            val txBuilder = TransactionBuilder(notary)
                    .addInputState(orderStateRef)
                    .addOutputState(outputState, NUMBER_CONTRACT_ID, notary)
                    .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            // Send Request Type
            val originSession: FlowSession? =
            if (outputState.origOperator != outputState.lastOperator
                    && outputState.origOperator != outputState.currOperator)
                initiateFlow(outputState.origOperator)
            else {
                null
            }
            val transferToSession = initiateFlow(outputState.currOperator)

            originSession?.send("orig")
            transferToSession.send("current")

            // Send the state to the counter party, and receive it back with their signature.
            val otherPartyFlows: List<FlowSession>? =
            if (originSession != null)
                listOf(originSession, transferToSession).distinct()
            else
                listOf(transferToSession).distinct()

            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, otherPartyFlows as List<FlowSession>, GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val request = otherPartyFlow.receive<String>().unwrap{it}
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Number transaction." using (output is NumberState)
                    val number = output as NumberState
                    when (request) {
                        "current" ->
                            "The current operator must myself." using
                                    (number.currOperator == serviceHub.myInfo.legalIdentities.first())
                    }
                }
            }
            return subFlow(signTransactionFlow)
        }
    }
}
