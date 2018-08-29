# Number Portability Cordapp (PoC)

# Flows

## NumberAccessFlow

```text
CTCC
>>> flow start com.cienet.npdapp.number.NumberAccessFlow$Initiator number: 18600000001
```

```text
CTCC, CMCC, CUCC
>>> run vaultQuery contractStateType: com.cienet.npdapp.number.NumberState
```

## NumberTransferFromFlow

CTCC -> CMCC

```text
CMCC
>>> flow start com.cienet.npdapp.number.NumberTransferFromFlow$Initiator number: 18600000001, otherParty: CTCC
```

```text
CTCC, CMCC, CUCC
>>> run vaultQuery contractStateType: com.cienet.npdapp.number.NumberState
```

CMCC -> CUCC

```text
CUCC
>>> flow start com.cienet.npdapp.number.NumberTransferFromFlow$Initiator number: 18600000001, otherParty: CMCC
```

# Compile

```bash
$ ./gradlew clean
$ ./gradlew deployNodes -Poffline=true
```

## Build Docker image

```bash
$ ./gradlew buildDocker
```

# Run

## In local tty

```bash
$ ./build/nodes/runnodes
```

## Docker container

```bash
UP
$ docker-compose -f src/main/docker/docker-compose.yml up -d
```

```bash
DOWN
$ docker-compose -f src/main/docker/docker-compose.yml down
```

# RPC Client

```bash
$ java -classpath out/production/classes com.cienet.npdapp.rpcclient.RPCClientKt --address 10.10.11.111:10046 --flow-name query --number 18600123456
```