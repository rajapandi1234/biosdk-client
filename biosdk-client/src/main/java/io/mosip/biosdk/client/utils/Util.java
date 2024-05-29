package io.mosip.biosdk.client.utils;

import static io.mosip.biosdk.client.constant.AppConstants.LOGGER_IDTYPE;
import static io.mosip.biosdk.client.constant.AppConstants.LOGGER_SESSIONID;

import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.mosip.biosdk.client.config.LoggerConfig;
import io.mosip.kernel.core.logger.spi.Logger;

public class Util {

	private static final GsonHttpMessageConverter MESSAGE_CONVERTER;

	private static final RestTemplate REST_TEMPLATE;

	static {
		MESSAGE_CONVERTER = new GsonHttpMessageConverter();
		REST_TEMPLATE = new RestTemplate();
		REST_TEMPLATE.getMessageConverters().add(MESSAGE_CONVERTER);
	}

	public static final String DEBUG_REQUEST_RESPONSE = System.getenv("mosip_biosdk_request_response_debug");

	private static Logger utilLogger = LoggerConfig.logConfig(Util.class);

	private Util() {
		throw new IllegalStateException("Util class");
	}

	@SuppressWarnings({ "java:S1452" })
	public static ResponseEntity<?> restRequest(String url, HttpMethod httpMethodType, MediaType mediaType, Object body,
			Map<String, String> headersMap, Class<?> responseClass) {
		ResponseEntity<?> response = null;
		RestTemplate restTemplate = getRestTemplate();

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(mediaType);
			HttpEntity<?> request = null;
			if (headersMap != null) {
				headersMap.forEach((k, v) -> headers.add(k, v));
			}
			if (body != null) {
				request = new HttpEntity<>(body, headers);
			} else {
				request = new HttpEntity<>(headers);
			}

			if (DEBUG_REQUEST_RESPONSE != null && DEBUG_REQUEST_RESPONSE.equalsIgnoreCase("y")) {
				Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
				utilLogger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, "Request: ", gson.toJson(request.getBody()));
			}

			response = restTemplate.exchange(url, httpMethodType, request, responseClass);

			Object responseBodyObject = response.getBody();
			String responseBody = responseBodyObject != null ? responseBodyObject.toString() : "";

			if (DEBUG_REQUEST_RESPONSE != null && DEBUG_REQUEST_RESPONSE.equalsIgnoreCase("y")) {
				utilLogger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, "Response: ", responseBody);
			}
		} catch (RestClientException ex) {
			ex.printStackTrace();
			throw new RestClientException("rest call failed");
		}
		return response;

	}

	private static RestTemplate getRestTemplate() {
		return REST_TEMPLATE;
	}

	public static String base64Encode(String data) {
		return Base64.getEncoder().encodeToString(data.getBytes());
	}
}
