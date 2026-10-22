package in.landledger.landledger_backend;

import java.net.URI;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "landledger.blockchain")
public class BlockchainProperties {

	private static final Pattern ETHEREUM_ADDRESS = Pattern.compile("0x[0-9a-fA-F]{40}");

	private boolean enabled;
	private String rpcUrl = "http://127.0.0.1:8545";
	private String fromAddress = "";
	private String contractAddress = "";
	private long pollIntervalMs = 5000;

	@PostConstruct
	void validate() {
		if (pollIntervalMs < 1000) {
			throw new IllegalStateException("LANDLEDGER_BLOCKCHAIN_POLL_INTERVAL_MS must be at least 1000");
		}
		if (enabled) {
			if (!ETHEREUM_ADDRESS.matcher(fromAddress).matches()) {
				throw new IllegalStateException("Set LANDLEDGER_BLOCKCHAIN_FROM_ADDRESS to the local chain account address");
			}
			if (!ETHEREUM_ADDRESS.matcher(contractAddress).matches()) {
				throw new IllegalStateException("Set LANDLEDGER_BLOCKCHAIN_CONTRACT_ADDRESS to the deployed contract address");
			}
			URI uri = URI.create(rpcUrl);
			if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
				throw new IllegalStateException("LANDLEDGER_BLOCKCHAIN_RPC_URL must use HTTP or HTTPS");
			}
			if (!"127.0.0.1".equals(uri.getHost()) && !"localhost".equals(uri.getHost())) {
				throw new IllegalStateException(
					"LANDLEDGER_BLOCKCHAIN_RPC_URL must point to this computer's local development chain");
			}
		}
	}

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public String getRpcUrl() { return rpcUrl; }
	public void setRpcUrl(String rpcUrl) { this.rpcUrl = rpcUrl; }
	public String getFromAddress() { return fromAddress; }
	public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
	public String getContractAddress() { return contractAddress; }
	public void setContractAddress(String contractAddress) { this.contractAddress = contractAddress; }
	public long getPollIntervalMs() { return pollIntervalMs; }
	public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
}
