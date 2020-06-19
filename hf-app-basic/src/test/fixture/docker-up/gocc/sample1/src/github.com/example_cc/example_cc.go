
package main

import (
	"fmt"
	"strconv"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

var logger = shim.NewLogger("example_cc0")

// SimpleChaincode example simple Chaincode implementation
type SimpleChaincode struct {
}


// Init initializes the chaincode state
func (t *SimpleChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {
	logger.Info("########### example_cc Init ###########")

	_, args := stub.GetFunctionAndParameters()

	logger.Info("########### example_cc Init args:" + string(args[0]))


	if transientMap, err := stub.GetTransient(); err == nil {

		rc := transientMap["rc"]

		transientData := transientMap["result"]

		if rc == nil {
			return shim.Success(transientData)
		}

		vrc, err := strconv.Atoi(string(rc[:]))

		if err != nil {
			return shim.Error(err.Error())
		}

		return pb.Response{
			Status:  int32(vrc),
			Payload: transientData,
		}

	}


	return shim.Success(nil)

}

// Invoke makes payment of X units from A to B
func (t *SimpleChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	logger.Info("########### example_cc Invoke ###########")

	function, args := stub.GetFunctionAndParameters()

	if function == "add" {
		// Add entity to the state
		return t.add(stub, args)
	}

	if function == "delete" {
		// Deletes an entity from its state
		return t.delete(stub, args)
	}

	if function == "query" {
		// queries an entity state
		return t.query(stub, args)
	}

	logger.Errorf("Unknown action, check the first argument, must be one of 'delete', 'query', or 'add'. But got: %v", args[0])
	return shim.Error(fmt.Sprintf("Unknown action, check the first argument, must be one of 'delete', 'query', or 'add'. But got: %v", args[0]))
}

func (t *SimpleChaincode) add(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var A, OwnerA string

	var err error

	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2: function followed by 1 name and 1 value")
	}

	A = args[0]

	// Get the state from the ledger
	OwnerAbytes, err := stub.GetState(A)

	if err != nil {
		return shim.Error("Error!")
	}
	if OwnerAbytes != nil {
		return shim.Error("Entity was registered earlier for owner:" + string(OwnerAbytes))
	}
	OwnerA = args[1]

	err = stub.PutState(A, []byte(OwnerA))
	if err != nil {
		return shim.Error(err.Error())
	}

	return shim.Success(nil)
}

// Deletes an entity from state
func (t *SimpleChaincode) delete(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var A string

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	A = args[0]

	// Delete the key from the state in ledger
	err := stub.DelState(A)
	if err != nil {
		return shim.Error("Failed to delete state")
	}

	return shim.Success(nil)
}

// Query callback representing the query of a chaincode
func (t *SimpleChaincode) query(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var A string

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting name of the person to query")
	}

	A = args[0]

	// Get the state from the ledger
	OwnerAbytes, err := stub.GetState(A)


	if err != nil {
		jsonResp := "{\"Error\":\"Failed to get state for " + A + "\"}"
		return shim.Error(jsonResp)
	}

	if OwnerAbytes == nil {
		jsonResp := "{\"Error\":\"Nil amount for " + A + "\"}"
		return shim.Error(jsonResp)
	}

	return shim.Success(OwnerAbytes)
}

func main() {
	err := shim.Start(new(SimpleChaincode))
	if err != nil {
		logger.Errorf("Error starting Simple chaincode: %s", err)
	}
}