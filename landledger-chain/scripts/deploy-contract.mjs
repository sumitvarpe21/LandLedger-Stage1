import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { ContractFactory, JsonRpcProvider } from "ethers";

const artifact = JSON.parse(
  readFileSync(resolve("artifacts/contracts/LandLedgerProof.sol/LandLedgerProof.json"), "utf8"),
);
const provider = new JsonRpcProvider("http://127.0.0.1:8545");
const signer = await provider.getSigner(0);
const contract = await new ContractFactory(artifact.abi, artifact.bytecode, signer).deploy();
await contract.waitForDeployment();

console.log("Copy these local-only settings into landledger-backend\\.env:");
console.log("LANDLEDGER_BLOCKCHAIN_ENABLED=true");
console.log("LANDLEDGER_BLOCKCHAIN_RPC_URL=http://127.0.0.1:8545");
console.log(`LANDLEDGER_BLOCKCHAIN_FROM_ADDRESS=${await signer.getAddress()}`);
console.log(`LANDLEDGER_BLOCKCHAIN_CONTRACT_ADDRESS=${await contract.getAddress()}`);
await provider.destroy();
