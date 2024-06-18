package io.mosip.biosdk.client.impl.spec_1_0;

import static io.mosip.biosdk.client.constant.AppConstants.LOGGER_IDTYPE;
import static io.mosip.biosdk.client.constant.AppConstants.LOGGER_SESSIONID;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import io.mosip.biosdk.client.config.LoggerConfig;
import io.mosip.biosdk.client.constant.ResponseStatus;
import io.mosip.biosdk.client.dto.CheckQualityRequestDto;
import io.mosip.biosdk.client.dto.ConvertFormatRequestDto;
import io.mosip.biosdk.client.dto.ErrorDto;
import io.mosip.biosdk.client.dto.ExtractTemplateRequestDto;
import io.mosip.biosdk.client.dto.InitRequestDto;
import io.mosip.biosdk.client.dto.MatchRequestDto;
import io.mosip.biosdk.client.dto.RequestDto;
import io.mosip.biosdk.client.dto.SegmentRequestDto;
import io.mosip.biosdk.client.exception.BioSdkClientException;
import io.mosip.biosdk.client.utils.Util;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.model.MatchDecision;
import io.mosip.kernel.biometrics.model.QualityCheck;
import io.mosip.kernel.biometrics.model.Response;
import io.mosip.kernel.biometrics.model.SDKInfo;
import io.mosip.kernel.biometrics.spi.IBioApiV2;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Implementation of the BioSDK client API version 1.0. This class provides
 * methods to interact with the BioSDK service for biometric operations.
 * 
 * <p>
 * Methods provided in this implementation include initializing the SDK,
 * performing quality checks, matching biometric records, extracting templates,
 * segmenting biometric data, and converting formats.
 * </p>
 * 
 * <p>
 * It utilizes Spring Framework for RESTful interactions and Gson library for
 * JSON handling.
 * </p>
 * 
 * <p>
 * Logging is handled using Mosip LoggerConfig for consistent and structured
 * logging.
 * </p>
 * 
 * @author Sanjay Murali
 * @author Manoj SP
 * @author Ankit
 * @author Loganathan Sekar
 * 
 * @since 1.0.0
 */
@SuppressWarnings({ "java:S101" })
public class Client_V_1_0 implements IBioApiV2 {
	private static Logger logger = LoggerConfig.logConfig(Client_V_1_0.class);

	private static final String FORMAT_SUFFIX = ".format";

	private static final String DEFAULT = "default";

	private static final String FORMAT_URL_PREFIX = "format.url.";

	private static final String MOSIP_BIOSDK_SERVICE = "mosip_biosdk_service";

	private static final String VERSION = "1.0";

	private static final String TAG_HTTP_URL = "HTTP url: ";
	private static final String TAG_HTTP_STATUS = "HTTP status: ";
	private static final String TAG_ERRORS = "errors";
	private static final String TAG_RESPONSE = "response";
	private static final String TAG_STATUS_CODE = "statusCode";
	private static final String TAG_STATUS_MESSAGE = "statusMessage";

	private Gson gson;

	private Type errorDtoListType;

	private Map<String, String> sdkUrlsMap;

	/**
	 * Constructs a new instance of the BioSDK client version 1.0. Initializes Gson
	 * for JSON serialization and deserialization.
	 */
	public Client_V_1_0() {
		gson = new GsonBuilder().serializeNulls().create();
		errorDtoListType = new TypeToken<List<ErrorDto>>() {
		}.getType();
	}

	/**
	 * Initializes the BioSDK client with the provided initialization parameters.
	 * Retrieves SDK URLs and aggregates SDK information from multiple sources.
	 * 
	 * @param initParams The initialization parameters for the SDK.
	 * @return SDKInfo object containing aggregated SDK information.
	 * @throws BioSdkClientException if initialization fails or SDK URLs are not
	 *                               configured properly.
	 */
	@Override
	public SDKInfo init(Map<String, String> initParams) {
		sdkUrlsMap = getSdkUrls(initParams);
		List<SDKInfo> sdkInfos = sdkUrlsMap.values().stream().map(sdkUrl -> initForSdkUrl(initParams, sdkUrl)).toList();
		return getAggregatedSdkInfo(sdkInfos);
	}

	/**
	 * Aggregates information from a list of SDKInfo objects into a single SDKInfo
	 * object.
	 * 
	 * If the provided list is empty, returns null. If the list contains a single
	 * element, returns that element directly. Otherwise, iterates through the list
	 * and merges information from each element to create a new aggregated SDKInfo
	 * object.
	 *
	 * @param sdkInfos The list of SDKInfo objects to aggregate.
	 * @return An SDKInfo object containing aggregated information, or null if the
	 *         list is empty.
	 */
	private SDKInfo getAggregatedSdkInfo(List<SDKInfo> sdkInfos) {
		SDKInfo sdkInfo;
		if (!sdkInfos.isEmpty()) {
			sdkInfo = sdkInfos.get(0);
			if (sdkInfos.size() == 1) {
				return sdkInfo;
			} else {
				return getAggregatedSdkInfo(sdkInfos, sdkInfo);
			}
		} else {
			sdkInfo = null;
		}
		return sdkInfo;
	}

	/**
	 * Helper method used for recursive aggregation of SDKInfo objects.
	 * 
	 * This method likely extracts common details (API version, SDK version,
	 * organization, type) from the provided initial SDKInfo object and creates a
	 * new aggregated SDKInfo instance. It then iterates through the remaining
	 * elements in the list (sdkInfos) and calls addOtherSdkInfoDetails to merge
	 * details from each element into the aggregated object.
	 *
	 * @param sdkInfos The list of SDKInfo objects to process.
	 * @param sdkInfo  An initial SDKInfo object to use as a base for aggregation.
	 * @return An SDKInfo object containing aggregated information.
	 */
	private SDKInfo getAggregatedSdkInfo(List<SDKInfo> sdkInfos, SDKInfo sdkInfo) {
		String organization = sdkInfo.getProductOwner() == null ? null : sdkInfo.getProductOwner().getOrganization();
		String type = sdkInfo.getProductOwner() == null ? null : sdkInfo.getProductOwner().getType();
		SDKInfo aggregatedSdkInfo = new SDKInfo(sdkInfo.getApiVersion(), sdkInfo.getSdkVersion(), organization, type);
		sdkInfos.forEach(info -> addOtherSdkInfoDetails(info, aggregatedSdkInfo));
		return aggregatedSdkInfo;
	}

	/**
	 * Merges specific details from a source SDKInfo object into a target aggregated
	 * SDKInfo object.
	 *
	 * This method selectively merges details like "other information" (maps),
	 * "supported methods" (maps), and "supported modalities" (lists) from the
	 * source object to the target object. It avoids adding duplicate entries for
	 * supported modalities.
	 *
	 * @param sdkInfo           The source SDKInfo object to extract details from.
	 * @param aggregatedSdkInfo The target SDKInfo object to merge details into.
	 */
	private void addOtherSdkInfoDetails(SDKInfo sdkInfo, SDKInfo aggregatedSdkInfo) {
		if (sdkInfo.getOtherInfo() != null) {
			aggregatedSdkInfo.getOtherInfo().putAll(sdkInfo.getOtherInfo());
		}
		if (sdkInfo.getSupportedMethods() != null) {
			aggregatedSdkInfo.getSupportedMethods().putAll(sdkInfo.getSupportedMethods());
		}
		if (sdkInfo.getSupportedModalities() != null) {
			List<BiometricType> supportedModalities = aggregatedSdkInfo.getSupportedModalities();
			supportedModalities.addAll(
					sdkInfo.getSupportedModalities().stream().filter(s -> !supportedModalities.contains(s)).toList());
		}
	}

	/**
	 * Initializes the SDK for a given SDK service URL using the provided
	 * initialization parameters.
	 * 
	 * @param initParams    The initialization parameters required for SDK
	 *                      initialization.
	 * @param sdkServiceUrl The URL of the SDK service to initialize.
	 * @return An {@code SDKInfo} object containing information about the
	 *         initialized SDK.
	 * @throws BioSdkClientException If an error occurs during SDK initialization or
	 *                               processing.
	 */
	private SDKInfo initForSdkUrl(Map<String, String> initParams, String sdkServiceUrl) {
		SDKInfo sdkInfo = null;
		try {
			InitRequestDto initRequestDto = new InitRequestDto();
			initRequestDto.setInitParams(initParams);

			RequestDto requestDto = generateNewRequestDto(initRequestDto);
			logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_URL, sdkServiceUrl + "/init");
			ResponseEntity<?> responseEntity = Util.restRequest(sdkServiceUrl + "/init", HttpMethod.POST,
					MediaType.APPLICATION_JSON, requestDto, null, String.class);
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_STATUS,
						responseEntity.getStatusCode().toString());
				throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR + "",
						TAG_HTTP_STATUS + responseEntity.getStatusCode().toString());
			}
			if (responseEntity.getBody() != null) {
				Object responseBodyObject = responseEntity.getBody();
				String responseBody = responseBodyObject != null ? responseBodyObject.toString() : "";
				JSONParser parser = new JSONParser();
				JSONObject js = (JSONObject) parser.parse(responseBody);

				/* Error handler */
				errorHandler(js.get(TAG_ERRORS) != null ? gson.fromJson(js.get(TAG_ERRORS).toString(), errorDtoListType)
						: null);

				sdkInfo = gson.fromJson(js.get(TAG_RESPONSE).toString(), SDKInfo.class);
			} else
				throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR + "", "Response is null");
		} catch (Exception e) {
			logger.error(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_URL, e);
			throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR + "", e.getLocalizedMessage(), e);
		}
		return sdkInfo;
	}

	/**
	 * Retrieves and returns SDK service URLs from the provided initialization
	 * parameters. If a default URL is not specified in the parameters, attempts to
	 * fetch it from the environment variables. Throws an exception if no valid SDK
	 * service URL is configured.
	 * 
	 * @param initParams The initialization parameters containing SDK service URLs.
	 * @return A {@code Map} containing SDK service names as keys and their
	 *         corresponding URLs as values.
	 * @throws IllegalStateException If no valid SDK service URL is configured.
	 */
	private Map<String, String> getSdkUrls(Map<String, String> initParams) {
		Map<String, String> sdkUrls = new HashMap<>(initParams.entrySet().stream()
				.filter(entry -> entry.getKey().contains(FORMAT_URL_PREFIX)).collect(Collectors
						.toMap(entry -> entry.getKey().substring(FORMAT_URL_PREFIX.length()), Entry::getValue)));
		if (!sdkUrls.containsKey(DEFAULT)) {
			// If default is not specified in configuration, try getting it from env.
			String defaultSdkServiceUrl = getDefaultSdkServiceUrlFromEnv();
			if (defaultSdkServiceUrl != null) {
				sdkUrls.put(DEFAULT, defaultSdkServiceUrl);
			}
		}

		// There needs a default URL to be used when no format is specified.
		if (!sdkUrls.containsKey(DEFAULT) && !sdkUrls.isEmpty()) {
			// Take any first url and set it to default
			sdkUrls.put(DEFAULT, sdkUrls.values().iterator().next());
		}

		if (sdkUrls.isEmpty()) {
			throw new IllegalStateException("No valid sdk service url configured");
		}
		return sdkUrls;
	}

	/**
	 * Retrieves the SDK service URL based on the specified biometric modality and
	 * optional flags. If a specific format for the modality is found in the flags
	 * map, returns the corresponding URL from the sdkUrlsMap. If no matching format
	 * is found, falls back to the default SDK service URL.
	 *
	 * @param modality The biometric modality for which the SDK service URL is
	 *                 requested.
	 * @param flags    Optional flags that may contain a specific format key for the
	 *                 modality.
	 * @return The SDK service URL corresponding to the modality and flags, or the
	 *         default SDK service URL if not found.
	 */
	private String getSdkServiceUrl(BiometricType modality, Map<String, String> flags) {
		if (modality != null) {
			String key = modality.name() + FORMAT_SUFFIX;
			if (flags != null) {
				Optional<String> formatFromFlag = flags.entrySet().stream()
						.filter(e -> e.getKey().equalsIgnoreCase(key)).findAny().map(Entry::getValue);
				if (formatFromFlag.isPresent()) {
					String format = formatFromFlag.get();
					Optional<String> urlForFormat = sdkUrlsMap.entrySet().stream()
							.filter(e -> e.getKey().equalsIgnoreCase(format)).findAny().map(Entry::getValue);
					if (urlForFormat.isPresent()) {
						return urlForFormat.get();
					}
				}
			}
		}
		return getDefaultSdkServiceUrl();
	}

	/**
	 * Retrieves the default SDK service URL from the sdkUrlsMap.
	 *
	 * @return The default SDK service URL.
	 */
	private String getDefaultSdkServiceUrl() {
		return sdkUrlsMap.get(DEFAULT);
	}

	/**
	 * Retrieves the default SDK service URL from the environment variables.
	 *
	 * @return The default SDK service URL fetched from the environment variables.
	 */
	private String getDefaultSdkServiceUrlFromEnv() {
		return System.getenv(MOSIP_BIOSDK_SERVICE);
	}

	/**
	 * Performs a quality check on the provided biometric sample for specified
	 * modalities using the configured SDK service.
	 *
	 * @param sample            The biometric record sample to be checked.
	 * @param modalitiesToCheck List of biometric modalities to perform quality
	 *                          check for.
	 * @param flags             Optional flags for customization of the quality
	 *                          check operation.
	 * @return A response containing the quality check result.
	 * @throws BioSdkClientException If an error occurs during the quality check
	 *                               operation.
	 */
	@Override
	public Response<QualityCheck> checkQuality(BiometricRecord sample, List<BiometricType> modalitiesToCheck,
			Map<String, String> flags) {
		Response<QualityCheck> response = new Response<>();
		response.setStatusCode(200);
		QualityCheck qualityCheck = null;
		try {
			CheckQualityRequestDto checkQualityRequestDto = new CheckQualityRequestDto();
			checkQualityRequestDto.setSample(sample);
			checkQualityRequestDto.setModalitiesToCheck(modalitiesToCheck);
			checkQualityRequestDto.setFlags(flags);
			RequestDto requestDto = generateNewRequestDto(checkQualityRequestDto);
			String url = getSdkServiceUrl(modalitiesToCheck.get(0), flags) + "/check-quality";
			logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_URL, url);
			ResponseEntity<?> responseEntity = Util.restRequest(url, HttpMethod.POST, MediaType.APPLICATION_JSON,
					requestDto, null, String.class);
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_STATUS,
						responseEntity.getStatusCode().toString());
				throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR.getStatusCode() + "",
						TAG_HTTP_STATUS + responseEntity.getStatusCode().toString());
			}
			Object responseBodyObject = responseEntity.getBody();
			String responseBody = responseBodyObject != null ? responseBodyObject.toString() : "";
			JSONParser parser = new JSONParser();
			JSONObject js = (JSONObject) parser.parse(responseBody);
			JSONObject responseJson = (JSONObject) ((JSONObject) js.get(TAG_RESPONSE)).get(TAG_RESPONSE);

			/* Error handler */
			errorHandler(
					js.get(TAG_ERRORS) != null ? gson.fromJson(js.get(TAG_ERRORS).toString(), errorDtoListType) : null);

			qualityCheck = gson.fromJson(responseJson.toString(), QualityCheck.class);
		} catch (Exception e) {
			logger.error(LOGGER_SESSIONID, LOGGER_IDTYPE, "checkQuality", e);
			throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR.getStatusCode() + "", e.getLocalizedMessage(),
					e);
		}
		response.setResponse(qualityCheck);
		return response;
	}

	/**
	 * Matches a biometric sample against a gallery of biometric records using the
	 * specified modalities and flags.
	 *
	 * @param sample            The biometric record sample to match against the
	 *                          gallery.
	 * @param gallery           Array of biometric records forming the gallery to
	 *                          match against.
	 * @param modalitiesToMatch List of biometric modalities to use for matching.
	 * @param flags             Optional flags for customization of the matching
	 *                          operation.
	 * @return A response containing the match decisions from the matching
	 *         operation.
	 * @throws BioSdkClientException If an error occurs during the matching
	 *                               operation.
	 */
	@Override
	public Response<MatchDecision[]> match(BiometricRecord sample, BiometricRecord[] gallery,
			List<BiometricType> modalitiesToMatch, Map<String, String> flags) {
		Response<MatchDecision[]> response = new Response<>();
		try {
			MatchRequestDto matchRequestDto = new MatchRequestDto();
			matchRequestDto.setSample(sample);
			matchRequestDto.setGallery(gallery);
			matchRequestDto.setModalitiesToMatch(modalitiesToMatch);
			matchRequestDto.setFlags(flags);

			RequestDto requestDto = generateNewRequestDto(matchRequestDto);
			String url = getSdkServiceUrl(modalitiesToMatch.get(0), flags) + "/match";
			logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_URL, url);
			ResponseEntity<?> responseEntity = Util.restRequest(url, HttpMethod.POST, MediaType.APPLICATION_JSON,
					requestDto, null, String.class);
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_STATUS,
						responseEntity.getStatusCode().toString());
				throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR + "",
						TAG_HTTP_STATUS + responseEntity.getStatusCode().toString());
			}
			Object responseBodyObject = responseEntity.getBody();
			String responseBody = responseBodyObject != null ? responseBodyObject.toString() : "";
			JSONParser parser = new JSONParser();
			JSONObject js = (JSONObject) parser.parse(responseBody);

			/* Error handler */
			errorHandler(
					js.get(TAG_ERRORS) != null ? gson.fromJson(js.get(TAG_ERRORS).toString(), errorDtoListType) : null);

			JSONObject jsonResponse = (JSONObject) parser.parse(js.get(TAG_RESPONSE).toString());
			response.setStatusCode(
					jsonResponse.get(TAG_STATUS_CODE) != null ? ((Long) jsonResponse.get(TAG_STATUS_CODE)).intValue()
							: null);
			response.setStatusMessage(
					jsonResponse.get(TAG_STATUS_MESSAGE) != null ? jsonResponse.get(TAG_STATUS_MESSAGE).toString()
							: "");
			response.setResponse(gson.fromJson(
					jsonResponse.get(TAG_RESPONSE) != null ? jsonResponse.get(TAG_RESPONSE).toString() : null,
					MatchDecision[].class));
		} catch (Exception e) {
			logger.error(LOGGER_SESSIONID, LOGGER_IDTYPE, "match", e);
			throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR.getStatusCode() + "", e.getLocalizedMessage(),
					e);
		}
		return response;
	}

	/**
	 * Extracts biometric templates from the provided sample using the specified
	 * modalities and flags.
	 *
	 * @param sample              The biometric record sample from which to extract
	 *                            templates.
	 * @param modalitiesToExtract List of biometric modalities for which to extract
	 *                            templates.
	 * @param flags               Optional flags for customization of the template
	 *                            extraction operation.
	 * @return A response containing the extracted biometric record with templates.
	 * @throws BioSdkClientException If an error occurs during the template
	 *                               extraction operation.
	 */
	@Override
	public Response<BiometricRecord> extractTemplate(BiometricRecord sample, List<BiometricType> modalitiesToExtract,
			Map<String, String> flags) {
		Response<BiometricRecord> response = new Response<>();
		try {
			ExtractTemplateRequestDto extractTemplateRequestDto = new ExtractTemplateRequestDto();
			extractTemplateRequestDto.setSample(sample);
			extractTemplateRequestDto.setModalitiesToExtract(modalitiesToExtract);
			extractTemplateRequestDto.setFlags(flags);

			RequestDto requestDto = generateNewRequestDto(extractTemplateRequestDto);
			String url = getSdkServiceUrl(modalitiesToExtract, flags) + "/extract-template";
			logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_URL, url);
			ResponseEntity<?> responseEntity = Util.restRequest(url, HttpMethod.POST, MediaType.APPLICATION_JSON,
					requestDto, null, String.class);
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_STATUS,
						responseEntity.getStatusCode().toString());
				throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR.getStatusCode() + "",
						TAG_HTTP_STATUS + responseEntity.getStatusCode().toString());
			}
			convertAndSetResponseObject(response, responseEntity);
		} catch (Exception e) {
			logger.error(LOGGER_SESSIONID, LOGGER_IDTYPE, "extractTemplate", e);
			throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR.getStatusCode() + "", e.getLocalizedMessage(),
					e);
		}
		return response;
	}

	/**
	 * Retrieves the SDK service URL based on the provided modalities to extract and
	 * optional flags. If modalities are specified, it fetches the URL corresponding
	 * to the first modality in the list. If no modalities are specified but flags
	 * are provided, it checks flags for specific biometric types (Finger, Iris,
	 * Face) and returns the corresponding SDK service URL if found. If neither
	 * modalities nor matching flags are provided, it returns the default SDK
	 * service URL.
	 *
	 * @param modalitiesToExtract List of biometric modalities for which to retrieve
	 *                            the SDK service URL.
	 * @param flags               Optional flags for customization of the SDK
	 *                            service URL retrieval.
	 * @return The SDK service URL based on the provided modalities and flags, or
	 *         the default URL if no matches are found.
	 */
	private String getSdkServiceUrl(List<BiometricType> modalitiesToExtract, Map<String, String> flags) {
		if (modalitiesToExtract != null && !modalitiesToExtract.isEmpty()) {
			return getSdkServiceUrl(modalitiesToExtract.get(0), flags);
		} else {
			Set<String> keySet = flags.keySet();
			for (String key : keySet) {
				if (key.toLowerCase().contains(BiometricType.FINGER.name().toLowerCase())) {
					return getSdkServiceUrl(BiometricType.FINGER, flags);
				} else if (key.toLowerCase().contains(BiometricType.IRIS.name().toLowerCase())) {
					return getSdkServiceUrl(BiometricType.IRIS, flags);
				} else if (key.toLowerCase().contains(BiometricType.FACE.name().toLowerCase())) {
					return getSdkServiceUrl(BiometricType.FACE, flags);
				}
			}
		}
		return getDefaultSdkServiceUrl();
	}

	/**
	 * Segments a biometric record into individual components using the specified
	 * modalities and flags.
	 *
	 * @param biometricRecord     The biometric record to be segmented.
	 * @param modalitiesToSegment List of biometric modalities to use for
	 *                            segmentation.
	 * @param flags               Optional flags for customization of the
	 *                            segmentation operation.
	 * @return A response containing the segmented biometric record components.
	 * @throws BioSdkClientException If an error occurs during the segmentation
	 *                               operation.
	 */
	@Override
	public Response<BiometricRecord> segment(BiometricRecord biometricRecord, List<BiometricType> modalitiesToSegment,
			Map<String, String> flags) {
		Response<BiometricRecord> response = new Response<>();
		try {
			SegmentRequestDto segmentRequestDto = new SegmentRequestDto();
			segmentRequestDto.setSample(biometricRecord);
			segmentRequestDto.setModalitiesToSegment(modalitiesToSegment);
			segmentRequestDto.setFlags(flags);

			RequestDto requestDto = generateNewRequestDto(segmentRequestDto);
			String url = getSdkServiceUrl(modalitiesToSegment.get(0), flags) + "/segment";
			logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_URL, url);
			ResponseEntity<?> responseEntity = Util.restRequest(url, HttpMethod.POST, MediaType.APPLICATION_JSON,
					requestDto, null, String.class);
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_STATUS,
						responseEntity.getStatusCode().toString());
				throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR.getStatusCode() + "",
						TAG_HTTP_STATUS + responseEntity.getStatusCode().toString());
			}
			convertAndSetResponseObject(response, responseEntity);
		} catch (Exception e) {
			logger.error(LOGGER_SESSIONID, LOGGER_IDTYPE, "segment", e);
			throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR.getStatusCode() + "", e.getLocalizedMessage(),
					e);
		}
		return response;
	}

	/**
	 * Converts the response entity into a structured Response object containing a
	 * BiometricRecord, handling any parsing errors and setting appropriate response
	 * properties.
	 *
	 * @param response       Response object to be populated with the converted
	 *                       BiometricRecord.
	 * @param responseEntity ResponseEntity object received from the SDK service API
	 *                       call.
	 * @throws ParseException        If there is an error parsing the response body.
	 * @throws BioSdkClientException If there is an error handling the response or
	 *                               converting it to BiometricRecord.
	 */
	private void convertAndSetResponseObject(Response<BiometricRecord> response, ResponseEntity<?> responseEntity)
			throws ParseException {
		Object responseBodyObject = responseEntity.getBody();
		String responseBody = responseBodyObject != null ? responseBodyObject.toString() : "";
		JSONParser parser = new JSONParser();
		JSONObject js = (JSONObject) parser.parse(responseBody);

		/* Error handler */
		errorHandler(
				js.get(TAG_ERRORS) != null ? gson.fromJson(js.get(TAG_ERRORS).toString(), errorDtoListType) : null);

		JSONObject jsonResponse = (JSONObject) parser.parse(js.get(TAG_RESPONSE).toString());
		response.setStatusCode(
				jsonResponse.get(TAG_STATUS_CODE) != null ? ((Long) jsonResponse.get(TAG_STATUS_CODE)).intValue()
						: null);
		response.setStatusMessage(
				jsonResponse.get(TAG_STATUS_MESSAGE) != null ? jsonResponse.get(TAG_STATUS_MESSAGE).toString() : "");
		response.setResponse(
				gson.fromJson(jsonResponse.get(TAG_RESPONSE) != null ? jsonResponse.get(TAG_RESPONSE).toString() : null,
						BiometricRecord.class));
	}

	/**
	 * Converts the format of a biometric record from the source format to the
	 * target format using the specified parameters and modalities for conversion.
	 * <p>
	 * Note: This method is deprecated and will be removed in future versions.
	 *
	 * @param sample              The biometric record to be converted.
	 * @param sourceFormat        The source format of the biometric record.
	 * @param targetFormat        The target format to which the biometric record
	 *                            should be converted.
	 * @param sourceParams        Optional parameters specific to the source format.
	 * @param targetParams        Optional parameters specific to the target format.
	 * @param modalitiesToConvert List of biometric modalities to include in the
	 *                            conversion process.
	 * @return The converted biometric record in the target format.
	 * @throws BioSdkClientException If an error occurs during the format conversion
	 *                               operation.
	 * @since 1.0.0
	 * @deprecated Since 1.2.0.1, for removal in a future release.
	 */
	@Deprecated(since = "1.2.0.1", forRemoval = true)
	@Override
	public BiometricRecord convertFormat(BiometricRecord sample, String sourceFormat, String targetFormat,
			Map<String, String> sourceParams, Map<String, String> targetParams,
			List<BiometricType> modalitiesToConvert) {
		BiometricRecord resBiometricRecord = null;
		try {
			ConvertFormatRequestDto convertFormatRequestDto = new ConvertFormatRequestDto();
			convertFormatRequestDto.setSample(sample);
			convertFormatRequestDto.setSourceFormat(sourceFormat);
			convertFormatRequestDto.setTargetFormat(targetFormat);
			convertFormatRequestDto.setSourceParams(sourceParams);
			convertFormatRequestDto.setTargetParams(targetParams);
			convertFormatRequestDto.setModalitiesToConvert(modalitiesToConvert);

			RequestDto requestDto = generateNewRequestDto(convertFormatRequestDto);
			String url = getDefaultSdkServiceUrl() + "/convert-format";
			logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_URL, url);
			ResponseEntity<?> responseEntity = Util.restRequest(url, HttpMethod.POST, MediaType.APPLICATION_JSON,
					requestDto, null, String.class);
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_STATUS,
						responseEntity.getStatusCode().toString());
				throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR.getStatusCode() + "",
						TAG_HTTP_STATUS + responseEntity.getStatusCode().toString());
			}
			Object responseBodyObject = responseEntity.getBody();
			String responseBody = responseBodyObject != null ? responseBodyObject.toString() : "";
			JSONParser parser = new JSONParser();
			JSONObject js = (JSONObject) parser.parse(responseBody);

			/* Error handler */
			errorHandler(
					js.get(TAG_ERRORS) != null ? gson.fromJson(js.get(TAG_ERRORS).toString(), errorDtoListType) : null);

			resBiometricRecord = gson.fromJson(js.get(TAG_RESPONSE).toString(), BiometricRecord.class);
		} catch (Exception e) {
			logger.error(LOGGER_SESSIONID, LOGGER_IDTYPE, "convertFormat", e);
			throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR.getStatusCode() + "", e.getLocalizedMessage(),
					e);
		}
		return resBiometricRecord;
	}

	/**
	 * Converts the format of a biometric record from the source format to the
	 * target format using the specified parameters and modalities for conversion.
	 * <p>
	 * This method sends a request to the SDK service endpoint for format conversion
	 * and handles the response. It expects a successful HTTP response (2xx status
	 * code) with a JSON payload containing the converted biometric record. If the
	 * conversion fails or an error occurs, a {@link BioSdkClientException} is
	 * thrown.
	 *
	 * @param sample              The biometric record to be converted.
	 * @param sourceFormat        The source format of the biometric record.
	 * @param targetFormat        The target format to which the biometric record
	 *                            should be converted.
	 * @param sourceParams        Optional parameters specific to the source format.
	 * @param targetParams        Optional parameters specific to the target format.
	 * @param modalitiesToConvert List of biometric modalities to include in the
	 *                            conversion process.
	 * @return A {@link Response} containing the converted biometric record in the
	 *         target format.
	 * @throws BioSdkClientException If an error occurs during the format conversion
	 *                               operation.
	 * @since 1.0.0
	 */
	@Override
	public Response<BiometricRecord> convertFormatV2(BiometricRecord sample, String sourceFormat, String targetFormat,
			Map<String, String> sourceParams, Map<String, String> targetParams,
			List<BiometricType> modalitiesToConvert) {
		Response<BiometricRecord> response = new Response<>();
		try {
			ConvertFormatRequestDto convertFormatRequestDto = new ConvertFormatRequestDto();
			convertFormatRequestDto.setSample(sample);
			convertFormatRequestDto.setSourceFormat(sourceFormat);
			convertFormatRequestDto.setTargetFormat(targetFormat);
			convertFormatRequestDto.setSourceParams(sourceParams);
			convertFormatRequestDto.setTargetParams(targetParams);
			convertFormatRequestDto.setModalitiesToConvert(modalitiesToConvert);

			RequestDto requestDto = generateNewRequestDto(convertFormatRequestDto);
			String url = getDefaultSdkServiceUrl() + "/convert-format";
			logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_URL, url);
			ResponseEntity<?> responseEntity = Util.restRequest(url, HttpMethod.POST, MediaType.APPLICATION_JSON,
					requestDto, null, String.class);
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				logger.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, TAG_HTTP_STATUS,
						responseEntity.getStatusCode().toString());
				throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR.getStatusCode() + "",
						TAG_HTTP_STATUS + responseEntity.getStatusCode().toString());
			}
			Object responseBodyObject = responseEntity.getBody();
			String responseBody = responseBodyObject != null ? responseBodyObject.toString() : "";
			convertAndSetResponseObject(response, responseBody, BiometricRecord.class);
		} catch (Exception e) {
			logger.error(LOGGER_SESSIONID, LOGGER_IDTYPE, "convertFormatV2", e);
			throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR.getStatusCode() + "", e.getLocalizedMessage(),
					e);
		}
		return response;
	}

	/**
	 * Parses the JSON response body into a structured format and sets the
	 * corresponding fields in the provided {@link Response} object.
	 *
	 * @param response     The {@link Response} object to set with parsed data.
	 * @param responseBody The JSON response body received from the SDK service
	 *                     endpoint.
	 * @param clazz        The class type of the expected response object.
	 * @param <T>          The generic type representing the response object.
	 * @throws ParseException If an error occurs while parsing the JSON response
	 *                        body.
	 * @since 1.0.0
	 */
	private <T> void convertAndSetResponseObject(Response<T> response, String responseBody, Class<T> clazz)
			throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject js = (JSONObject) parser.parse(responseBody);

		/* Error handler */
		errorHandler(
				js.get(TAG_ERRORS) != null ? gson.fromJson(js.get(TAG_ERRORS).toString(), errorDtoListType) : null);

		/* Error handler */
		errorHandler(
				js.get(TAG_ERRORS) != null ? gson.fromJson(js.get(TAG_ERRORS).toString(), errorDtoListType) : null);

		JSONObject jsonResponse = (JSONObject) parser.parse(js.get(TAG_RESPONSE).toString());
		response.setStatusCode(
				jsonResponse.get(TAG_STATUS_CODE) != null ? ((Long) jsonResponse.get(TAG_STATUS_CODE)).intValue()
						: null);
		response.setStatusMessage(
				jsonResponse.get(TAG_STATUS_MESSAGE) != null ? jsonResponse.get(TAG_STATUS_MESSAGE).toString() : "");
		response.setResponse(gson.fromJson(
				jsonResponse.get(TAG_RESPONSE) != null ? jsonResponse.get(TAG_RESPONSE).toString() : null, clazz));
	}

	/**
	 * Generates a new {@link RequestDto} object with the provided body, encoded as
	 * a base64 JSON string.
	 *
	 * @param body The object to be serialized and encoded as the request body.
	 * @return A new {@link RequestDto} instance configured with the serialized and
	 *         encoded request body.
	 * @since 1.0.0
	 */
	private RequestDto generateNewRequestDto(Object body) {
		RequestDto requestDto = new RequestDto();
		requestDto.setVersion(VERSION);
		requestDto.setRequest(Util.base64Encode(gson.toJson(body)));
		return requestDto;
	}

	/**
	 * Handles errors received from the SDK service by processing the list of
	 * {@link ErrorDto}. If errors are present, constructs an error message
	 * containing error codes and messages, and throws a
	 * {@link BioSdkClientException} with the concatenated error details.
	 *
	 * @param errors The list of {@link ErrorDto} objects representing errors
	 *               returned by the SDK service. If {@code null}, no action is
	 *               taken.
	 * @throws BioSdkClientException If errors are present, constructs an exception
	 *                               with the concatenated error details.
	 * @since 1.0.0
	 */
	private void errorHandler(List<ErrorDto> errors) {
		if (errors == null) {
			return;
		}

		StringBuilder errorMessages = new StringBuilder();
		for (ErrorDto errorDto : errors) {
			if (errorDto != null) {
				errorMessages.append("Code: ").append(errorDto.getCode()).append(", Message: ")
						.append(errorDto.getMessage()).append(System.lineSeparator());
			}
		}
		if (errorMessages.length() > 0) {
			throw new BioSdkClientException(ResponseStatus.UNKNOWN_ERROR.getStatusCode() + "",
					errorMessages.toString());
		}
	}
}