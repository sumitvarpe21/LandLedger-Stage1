package in.landledger.landledger_backend;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

@Service
public class BlockchainProofService {

	private static final Logger logger = LoggerFactory.getLogger(BlockchainProofService.class);
	private static final BigInteger LOCAL_CHAIN_ID = BigInteger.valueOf(1337);
	private static final BigInteger GAS_PRICE = BigInteger.valueOf(2_000_000_000L);
	private static final BigInteger GAS_LIMIT = BigInteger.valueOf(200_000);

	private final OwnershipHistoryRepository history;
	private final BlockchainProperties properties;
	private final JsonMapper jsonMapper;

	public BlockchainProofService(
			OwnershipHistoryRepository history,
			BlockchainProperties properties,
			JsonMapper jsonMapper) {
		this.history = history;
		this.properties = properties;
		this.jsonMapper = jsonMapper;
	}

	void queue(OwnershipHistoryEntry entry) {
		if (!properties.isEnabled()) {
			return;
		}
		try {
			String payload = jsonMapper.writeValueAsString(new ProofPayload(
				1,
				entry.getId(),
				entry.getEvent(),
				entry.getParcel().getId(),
				entry.getParcel().getParcelId(),
				entry.getOwner().getId(),
				entry.getOccurredAt(),
				UUID.randomUUID().toString()));
			entry.queueBlockchainProof(payload, sha256(payload));
		} catch (JacksonException | NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Could not prepare blockchain proof", exception);
		}
	}

	@Transactional
	public void submit(UUID historyId) {
		OwnershipHistoryEntry entry = history.findById(historyId)
			.orElseThrow(() -> new IllegalStateException("Blockchain proof history entry disappeared"));
		if (!"PENDING".equals(entry.getBlockchainStatus())) {
			return;
		}

		Web3j web3j = Web3j.build(new HttpService(properties.getRpcUrl()));
		try {
			if (!LOCAL_CHAIN_ID.equals(web3j.ethChainId().send().getChainId())) {
				entry.failBlockchainProof("RPC endpoint is not the configured local development chain");
				logger.error("RPC endpoint returned an unexpected chain ID for history entry {}", historyId);
				return;
			}
			if (entry.getTransactionReference() == null) {
				submitTransaction(web3j, entry);
				if (!"PENDING".equals(entry.getBlockchainStatus())) {
					return;
				}
			}
			confirmIfMined(web3j, entry);
		} catch (IOException exception) {
			entry.recordBlockchainAttempt("Local blockchain RPC unavailable; retry scheduled");
			logger.warn("Could not submit blockchain proof for history entry {}; retrying",
				historyId, exception);
		} finally {
			web3j.shutdown();
		}
	}

	private void submitTransaction(Web3j web3j, OwnershipHistoryEntry entry) throws IOException {
		Function function = new Function("anchor", List.of(
			new Bytes32(uuidBytes32(entry.getId())),
			new Bytes32(Numeric.hexStringToByteArray(entry.getBlockchainPayloadHash()))), List.of());
		String data = FunctionEncoder.encode(function);
		Transaction transaction = new Transaction(
			properties.getFromAddress(),
			null,
			GAS_PRICE,
			GAS_LIMIT,
			properties.getContractAddress(),
			BigInteger.ZERO,
			data);
		EthSendTransaction response = web3j.ethSendTransaction(transaction).send();
		if (response.hasError()) {
			entry.failBlockchainProof(limitError(response.getError().getMessage()));
			logger.error("Blockchain rejected proof for history entry {}: {}",
				entry.getId(), response.getError().getMessage());
			return;
		}
		String transactionHash = response.getTransactionHash();
		if (transactionHash == null || !transactionHash.matches("0x[0-9a-fA-F]{64}")) {
			entry.failBlockchainProof("Blockchain RPC returned an invalid transaction hash");
			logger.error("Blockchain RPC returned an invalid transaction hash for history entry {}",
				entry.getId());
			return;
		}
		entry.setTransactionReference(transactionHash);
		entry.recordBlockchainAttempt("Transaction submitted; waiting for confirmation");
	}

	private void confirmIfMined(Web3j web3j, OwnershipHistoryEntry entry) throws IOException {
		EthGetTransactionReceipt response = web3j
			.ethGetTransactionReceipt(entry.getTransactionReference())
			.send();
		Optional<TransactionReceipt> receipt = response.getTransactionReceipt();
		if (receipt.isEmpty()) {
			entry.recordBlockchainAttempt("Transaction is not mined yet; retry scheduled");
			return;
		}
		if (!receipt.get().isStatusOK()) {
			entry.failBlockchainProof("Blockchain transaction reverted");
			logger.error("Blockchain transaction {} reverted for history entry {}",
				entry.getTransactionReference(), entry.getId());
			return;
		}
		entry.confirmBlockchainProof(entry.getTransactionReference());
		logger.info("Anchored history entry {} in transaction {}",
			entry.getId(), entry.getTransactionReference());
	}

	private byte[] uuidBytes32(UUID id) {
		return Numeric.hexStringToByteArray("0x" + id.toString().replace("-", "") + "0".repeat(32));
	}

	private String sha256(String payload) throws NoSuchAlgorithmException {
		byte[] digest = MessageDigest.getInstance("SHA-256")
			.digest(payload.getBytes(StandardCharsets.UTF_8));
		return Numeric.toHexString(digest);
	}

	private String limitError(String error) {
		if (error == null) {
			return "Blockchain RPC rejected the transaction";
		}
		return error.length() > 500 ? error.substring(0, 500) : error;
	}

	private record ProofPayload(
			int version,
			UUID historyEntryId,
			String event,
			UUID parcelDatabaseId,
			String parcelId,
			UUID ownerId,
			Instant occurredAt,
			String salt) {
	}
}
