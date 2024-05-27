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
 * The Class BioApiImpl.
 * 
 * @author Sanjay Murali
 * @author Manoj SP
 * @author Ankit
 * @author Loganathan Sekar
 * 
 */
public class Client_V_1_0 // NOSONAR
		implements IBioApiV2 {
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

	public Client_V_1_0() {
		gson = new GsonBuilder().serializeNulls().create();
		errorDtoListType = new TypeToken<List<ErrorDto>>() {
		}.getType();
	}

	@Override
	public SDKInfo init(Map<String, String> initParams) {
		sdkUrlsMap = getSdkUrls(initParams);
		List<SDKInfo> sdkInfos = sdkUrlsMap.values().stream().map(sdkUrl -> initForSdkUrl(initParams, sdkUrl)).toList();
		return getAggregatedSdkInfo(sdkInfos);
	}

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

	private SDKInfo getAggregatedSdkInfo(List<SDKInfo> sdkInfos, SDKInfo sdkInfo) {
		String organization = sdkInfo.getProductOwner() == null ? null : sdkInfo.getProductOwner().getOrganization();
		String type = sdkInfo.getProductOwner() == null ? null : sdkInfo.getProductOwner().getType();
		SDKInfo aggregatedSdkInfo = new SDKInfo(sdkInfo.getApiVersion(), sdkInfo.getSdkVersion(), organization, type);
		sdkInfos.forEach(info -> addOtherSdkInfoDetails(info, aggregatedSdkInfo));
		return aggregatedSdkInfo;
	}

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

	private String getDefaultSdkServiceUrl() {
		return sdkUrlsMap.get(DEFAULT);
	}

	private String getDefaultSdkServiceUrlFromEnv() {
		return System.getenv(MOSIP_BIOSDK_SERVICE);
	}

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
	 * This method is deprecated and will be removed in future versions.
	 *
	 * @since 1.2.0.1
	 * @deprecated since 1.2.0.1, for removal in a future release
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

	private RequestDto generateNewRequestDto(Object body) {
		RequestDto requestDto = new RequestDto();
		requestDto.setVersion(VERSION);
		requestDto.setRequest(Util.base64Encode(gson.toJson(body)));
		return requestDto;
	}

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