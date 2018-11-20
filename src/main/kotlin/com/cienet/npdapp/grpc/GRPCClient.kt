package com.cienet.npdapp.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger


class GRPCClient

internal constructor(private val channel: ManagedChannel) {
    private val blockingStub: NPGRPCAdapterGrpc.NPGRPCAdapterBlockingStub
            = NPGRPCAdapterGrpc.newBlockingStub(channel)

    constructor(host: String, port: Int) : this(ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()) {
    }


    @Throws(InterruptedException::class)
    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    fun processNPReq(input: List<String>) {
        logger.log(Level.INFO, "Will try to greet {0}...", input)
        val request = NPRequest.newBuilder().addAllInputs(input).build()
        val response: NPReply =  try {
            blockingStub.processNPReq(request)
        } catch (e: StatusRuntimeException) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.status)
            return
        }

        logger.info("processNPReq: ${response.output}")
    }

    companion object {
        private val logger = Logger.getLogger(GRPCClient::class.java.name)

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val client = GRPCClient("localhost", 50051)
            try {
                client.processNPReq(args.toList())
            } finally {
                client.shutdown()
            }
        }
    }
}