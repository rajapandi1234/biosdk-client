package io.mosip.biosdk.client.dto;

import java.util.List;
import java.util.Map;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Data Transfer Object (DTO) for requesting biometric quality check in a
 * biometric SDK. Encapsulates parameters required for performing quality check
 * on biometric samples.
 *
 * <p>
 * This class represents a request to check the quality of a biometric sample,
 * specifying the sample biometric record, modalities to be checked, and
 * optional flags.
 * </p>
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@ToString
public class CheckQualityRequestDto {
	/**
	 * The biometric sample record for which quality is to be checked.
	 */
	private BiometricRecord sample;

	/**
	 * List of biometric modalities to be checked for quality.
	 */
	private List<BiometricType> modalitiesToCheck;

	/**
	 * Additional flags or parameters for quality checking, if any.
	 */
	private Map<String, String> flags;
}