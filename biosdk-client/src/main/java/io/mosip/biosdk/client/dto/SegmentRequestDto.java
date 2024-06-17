package io.mosip.biosdk.client.dto;

import java.util.List;
import java.util.Map;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Data Transfer Object (DTO) representing a request to segment biometric
 * records. Encapsulates the sample biometric record, list of biometric types to
 * segment, and additional flags for segmentation.
 *
 * <p>
 * This class facilitates transferring data related to biometric segmentation
 * operations between different layers of the application, such as client and
 * service layers. It contains necessary getters and setters for accessing its
 * attributes.
 * </p>
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@ToString
public class SegmentRequestDto {

	/**
	 * The sample biometric record to be segmented.
	 */
	private BiometricRecord sample;

	/**
	 * List of biometric types specifying modalities to be segmented.
	 */
	private List<BiometricType> modalitiesToSegment;

	/**
	 * Additional flags or parameters for segmentation.
	 */
	private Map<String, String> flags;
}