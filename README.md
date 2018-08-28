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

# Compile and run

```bash
$ ./gradlew clean
$ ./gradlew deployNodes -Poffline=true
$ ./build/nodes/runnodes
```