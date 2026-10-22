package in.landledger.landledger_backend;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class BlockchainProofWorker {

	private final BlockchainProperties properties;
	private final OwnershipHistoryRepository history;
	private final BlockchainProofService proofs;

	BlockchainProofWorker(
			BlockchainProperties properties,
			OwnershipHistoryRepository history,
			BlockchainProofService proofs) {
		this.properties = properties;
		this.history = history;
		this.proofs = proofs;
	}

	@Scheduled(fixedDelayString = "${landledger.blockchain.poll-interval-ms:5000}")
	void submitPendingProofs() {
		if (!properties.isEnabled()) {
			return;
		}
		history.findTop20ByBlockchainStatusOrderByOccurredAtAsc("PENDING")
			.forEach(entry -> proofs.submit(entry.getId()));
	}
}
