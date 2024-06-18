package io.mosip.biosdk.client.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * Custom Exception Class in case of error occurred in services.
 * 
 * @see io.mosip.kernel.core.exception.BaseUncheckedException
 * @author Janardhan B S
 * @since 1.0.0
 */
public class BioSdkClientException extends BaseUncheckedException {

	/**
	 * Constructs a BioSdkClientException with an error code and message.
	 *
	 * @param errorCode    The error code associated with the exception.
	 * @param errorMessage The error message describing the exception.
	 */
	public BioSdkClientException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Constructs a BioSdkClientException with an error code, message, and root
	 * cause exception.
	 *
	 * @param errorCode    The error code associated with the exception.
	 * @param errorMessage The error message describing the exception.
	 * @param rootCause    The underlying cause (throwable) that led to this
	 *                     exception.
	 */
	public BioSdkClientException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}
}