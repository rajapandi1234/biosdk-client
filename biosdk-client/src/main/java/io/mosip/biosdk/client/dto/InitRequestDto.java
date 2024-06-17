package io.mosip.biosdk.client.dto;

import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Data Transfer Object (DTO) representing a request to initialize a biometric
 * SDK. Encapsulates initialization parameters required for setting up the SDK.
 *
 * <p>
 * This class is used to structure requests for initializing a biometric SDK,
 * containing a map of initialization parameters needed for SDK setup.
 * </p>
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@ToString
public class InitRequestDto {
	/**
	 * Map of initialization parameters required for setting up the biometric SDK.
	 */
	private Map<String, String> initParams;
}