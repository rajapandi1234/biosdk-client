package io.mosip.biosdk.client.dto;

import java.util.List;
import java.util.Map;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Data Transfer Object (DTO) for requesting biometric data format conversion in
 * a biometric SDK. Encapsulates parameters required for converting biometric
 * data from one format to another.
 *
 * <p>
 * This class represents a request to convert the format of biometric data,
 * specifying the sample biometric record, source and target formats, source and
 * target parameters, and modalities to be converted.
 * </p>
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@ToString
public class ConvertFormatRequestDto {
	/**
	 * The sample biometric record to be converted.
	 */
	private BiometricRecord sample;

	/**
	 * The source format of the biometric data.
	 */
	private String sourceFormat;

	/**
	 * The target format to which the biometric data should be converted.
	 */
	private String targetFormat;

	/**
	 * Parameters specific to the source biometric data format.
	 */
	private Map<String, String> sourceParams;

	/**
	 * Parameters specific to the target biometric data format.
	 */
	private Map<String, String> targetParams;

	/**
	 * List of biometric modalities to be converted.
	 */
	private List<BiometricType> modalitiesToConvert;
}