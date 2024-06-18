package io.mosip.biosdk.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Data Transfer Object (DTO) representing a request to a service operation.
 * Encapsulates the version of the request format and the actual request data.
 *
 * <p>
 * This class is used to structure requests sent to services, containing the
 * version of the request format and the serialized request data.
 * </p>
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@ToString
public class RequestDto {

	/**
	 * The version of the request format.
	 */
	private String version;

	/**
	 * The serialized request data in JSON or another format.
	 */
	private String request;
}