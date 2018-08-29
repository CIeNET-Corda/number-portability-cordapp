package com.cienet.npdapp.rpcclient

import com.cienet.npdapp.number.*

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowException
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor

import org.slf4j.Logger

/**
 *  Demonstration of using the CordaRPCClient to connect to a Corda Node and
 *  steam some State data from the node.
 **/

fun main(args: Array<String>) {
    RPCClient().main(args)
}

private class RPCClient: CliktCommand() {
    companion object {
        val logger: Logger = loggerFor<RPCClient>()
        private fun logState(state: StateAndRef<NumberState>) = logger.info("{}", state.state.data)
    }

    private var cordaRPCOps: CordaRPCOps? = null

    fun connect(address: String, username: String, password: String) {
        val nodeAddress = NetworkHostAndPort.parse(address)
        val client = CordaRPCClient(nodeAddress)

        // Can be amended in the com.example.MainKt file.
        cordaRPCOps = client.start(username, password).proxy
    }

    fun queryStateBy(number: String): StateAndRef<NumberState> {
        val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results = cordaRPCOps?.vaultQueryByCriteria(criteria, NumberState::class.java)
        val orderStateRef = results?.states?.stream()
                ?.filter{it.state.data.number == number && it.state.data.currOperator == cordaRPCOps?.nodeInfo()?.legalIdentities?.first()}
                ?.findAny()
                ?.orElse(null) ?: throw FlowException("Can not find this number belongs to the this party.")
        return orderStateRef
    }

    fun performAccessFlow(number: String): StateAndRef<NumberState> {
        cordaRPCOps?.startTrackedFlow(NumberAccessFlow::Initiator, number)?.returnValue?.getOrThrow()
        return queryStateBy(number)
    }

    fun performTransferFromFlow(number: String, partyName: CordaX500Name): StateAndRef<NumberState> {
        val otherParty = cordaRPCOps?.wellKnownPartyFromX500Name(partyName) ?: throw FlowException("Can not find this party.")

        cordaRPCOps?.startTrackedFlow(NumberTransferFromFlow::Initiator, number, otherParty)?.returnValue?.getOrThrow()

        return queryStateBy(number)
    }

    val address: String by option(help="Address of RPC").default("127.0.0.1:10005")
    val username: String by option(help="The user to login").default("user1")
    val password: String by option(help="The password").default("test")
    val flowName: String by argument(help="The Flow Name [query|access|transfer]")
    val number: String by argument(help="The number for the flow action, e.g. 18612345678")
    val partyName: String by option(help="The CordaX500Name of the party for the flow action").default("O=CTCC, L=Beijing, C=CN")

    override fun run() {
        connect(address, username, password)

        when (flowName) {
            // --address 10.10.11.111:10006 --flow-name query --number 18612345678
            "query" -> {
                logState(queryStateBy(number))
            }
            // CMCC
            // --address 10.10.11.111:10006 --flow-name access --number 18612345678
            "access" -> {
                logState(performAccessFlow(number))
            }
            // CUCC
            // --address 10.10.11.111:10009 --flow-name transfer --number 18612345678 --party-name "O=CMCC, L=Beijing, C=CN"
            "transfer" -> {
                logState(performTransferFromFlow(number, CordaX500Name.parse(partyName)))
            }
        }

    }
}
