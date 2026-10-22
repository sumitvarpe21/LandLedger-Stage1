// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract LandLedgerProof {
    address public immutable owner;
    mapping(bytes32 => bytes32) public proofHashes;

    event RecordAnchored(
        bytes32 indexed recordId,
        bytes32 indexed payloadHash,
        uint256 anchoredAt
    );

    error NotOwner();
    error ConflictingProof();

    constructor() {
        owner = msg.sender;
    }

    function anchor(bytes32 recordId, bytes32 payloadHash) external {
        if (msg.sender != owner) revert NotOwner();

        bytes32 existingHash = proofHashes[recordId];
        if (existingHash != bytes32(0)) {
            if (existingHash != payloadHash) revert ConflictingProof();
            return;
        }

        proofHashes[recordId] = payloadHash;
        emit RecordAnchored(recordId, payloadHash, block.timestamp);
    }
}
