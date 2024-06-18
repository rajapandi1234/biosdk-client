package io.mosip.biosdk.client.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Data Transfer Object (DTO) representing a response from a service operation.
 * Encapsulates the version, response time, actual response object, and any
 * errors.
 *
 * <p>
 * This class is used to structure responses received from service invocations,
 * containing the version of the response, the time it was generated, the main
 * response data, and a list of any errors encountered during the operation.
 * </p>
 *
 * @param <T> The type of the response object encapsulated within this DTO.
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@ToString
public class ResponseDto<T> {

	/**
	 * The version of the response format.
	 */
	private String version;

	/**
	 * The timestamp indicating when the response was generated.
	 */
	private String responsetime;

	/**
	 * The main response object encapsulated within this DTO.
	 */
	private T response;

	/**
	 * List of errors encountered during the service operation.
	 */
	private List<ErrorDto> errors;
}