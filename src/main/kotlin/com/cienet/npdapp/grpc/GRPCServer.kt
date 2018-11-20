package com.cienet.npdapp.grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

class GRPCServer {

    private var server: Server? = null
    @Throws(IOException::class)
    private fun start() {
        val port = 50051
        server = ServerBuilder.forPort(port)
                .addService(NPGRPCAdapterImpl())
                .build()
                .start()
        logger.log(Level.INFO, "Server started, listening on {0}", port)
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down")
                this@GRPCServer.stop()
                System.err.println("*** server shut down")
            }
        })
    }
    private fun stop() {
        server?.shutdown()
    }

    @Throws(InterruptedException::class)
    private fun blockUntilShutdown() {
        server?.awaitTermination()
    }
    internal class NPGRPCAdapterImpl : NPGRPCAdapterGrpc.NPGRPCAdapterImplBase() {
        override fun processNPReq(req: NPRequest, responseObserver: StreamObserver<NPReply>) {
            val output = req.getInputsList().joinToString(", ")
            val reply = NPReply.newBuilder().setOutput("NPReply ${output}").build()
            responseObserver.onNext(reply)
            responseObserver.onCompleted()
        }
    }
    companion object {
        private val logger = Logger.getLogger(GRPCServer::class.java.name)
        /**
         * Main launches the server from the command line.
         */
        @Throws(IOException::class, InterruptedException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val server = GRPCServer()
            server.start()
            server.blockUntilShutdown()
        }
    }
}