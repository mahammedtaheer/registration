package io.mosip.registration.mdm.dto;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.constants.MosipBioDeviceConstants;
import io.mosip.registration.mdm.integrator.IMosipBioDeviceIntegrator;
import io.mosip.registration.mdm.service.impl.MosipBioDeviceManager;
import io.mosip.registration.mdm.util.MdmRequestResponseBuilder;
import lombok.Getter;
import lombok.Setter;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

/**
 * Holds the Biometric Device details
 * 
 * @author balamurugan.ramamoorthy
 *
 */
@Getter
@Setter
public class BioDevice {

	private String deviceType;
	private String deviceSubType;
	private String deviceModality;
	private int runningPort;
	private String runningUrl;
	private String status;
	private String providerName;
	private String providerId;
	private String serialVersion;
	private String certification;
	private String callbackId;
	private String deviceModel;
	private String deviceMake;
	private String firmWare;
	private String deviceExpiry;
	private String deviceId;
	private int deviceSubId;
	private String deviceProviderName;
	private String deviceProviderId;
	private String timestamp;

	private Map<String, Integer> deviceSubIdMapper = new HashMap<String, Integer>() {
		{

			put("LEFT", 1);
			put("RIGHT", 2);
			put("THUMBS", 3);
			put("FACE", 0);
			put("DOUBLE", 3);
			put("SINGLE",0);
		}
	};

	private static final Logger LOGGER = AppConfig.getLogger(BioDevice.class);
	private IMosipBioDeviceIntegrator mosipBioDeviceIntegrator;

	public CaptureResponseDto capture(RequestDetail requestDetail) throws RegBaseCheckedException, IOException {
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Entering into Capture method....."+ System.currentTimeMillis());
		String url = runningUrl + ":" + runningPort + "/" + MosipBioDeviceConstants.CAPTURE_ENDPOINT;

		CaptureResponseDto captureResponse = null;

		/* build the request object for capture */
		CaptureRequestDto mosipBioCaptureRequestDto = MdmRequestResponseBuilder.buildMosipBioCaptureRequestDto(this,requestDetail);
		String requestBody = null;
		ObjectMapper mapper = new ObjectMapper();
		requestBody = mapper.writeValueAsString(mosipBioCaptureRequestDto);
		CloseableHttpClient client = HttpClients.createDefault();
		StringEntity requestEntity = new StringEntity(requestBody, ContentType.create("Content-Type", Consts.UTF_8));
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Bulding capture url...."+ System.currentTimeMillis());
		HttpUriRequest request = RequestBuilder.create(requestDetail.getMosipProcess().equals("Registration")?"RCAPTURE":"CAPTURE").setUri(url).setEntity(requestEntity).build();
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Requesting capture url...."+ System.currentTimeMillis());
		CloseableHttpResponse response = client.execute(request);
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Request completed.... "+ System.currentTimeMillis());
		captureResponse = mapper.readValue(EntityUtils.toString(response.getEntity()).getBytes(StandardCharsets.UTF_8),
				CaptureResponseDto.class);
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Response Recived.... "+ System.currentTimeMillis());
		decode(captureResponse);
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Response Decode and leaving the method.... "+ System.currentTimeMillis());
		return captureResponse;

	}

	private void decode(CaptureResponseDto mosipBioCaptureResponseDto)
			throws IOException, JsonParseException, JsonMappingException {
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Entering into Decode Method.... "+ System.currentTimeMillis());
		ObjectMapper mapper = new ObjectMapper();
		if (null != mosipBioCaptureResponseDto && null != mosipBioCaptureResponseDto.getMosipBioDeviceDataResponses()) {
			for (CaptureResponseBioDto captureResponseBioDto : mosipBioCaptureResponseDto
					.getMosipBioDeviceDataResponses()) {
				if (null != captureResponseBioDto) {
					String bioJson = new String(Base64.getDecoder().decode(captureResponseBioDto.getCaptureBioData()));
					if (null != bioJson) {
						CaptureResponsBioDataDto captureResponsBioDataDto = mapper.readValue(bioJson.getBytes(),
								CaptureResponsBioDataDto.class);
						captureResponseBioDto.setCaptureResponseData(captureResponsBioDataDto);
					}
				}
			}
		}
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Leaving into Decode Method.... "+ System.currentTimeMillis());
	}

	public InputStream stream(RequestDetail requestDetail) throws IOException {
		
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Entering into Stream Method.... "+ System.currentTimeMillis());
		
		String url = runningUrl + ":" + runningPort + "/" + MosipBioDeviceConstants.STREAM_ENDPOINT;

		/* build the request object for capture */
		CaptureRequestDto mosipBioCaptureRequestDto = MdmRequestResponseBuilder.buildMosipBioCaptureRequestDto(this, requestDetail);

		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setRequestMethod("POST");
		String request = new ObjectMapper().writeValueAsString(mosipBioCaptureRequestDto);

		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(request);
		wr.flush();
		wr.close();
		con.setReadTimeout(5000);
		con.connect();
		InputStream urlStream = con.getInputStream();
		LOGGER.info("BioDevice", APPLICATION_NAME, APPLICATION_ID, "Leaving into Stream Method.... "+ System.currentTimeMillis());
		return urlStream;

	}

	public byte[] forceCapture() {
		return null;

	}

	public void buildDeviceSubId(String slapType) {
		setDeviceSubId(deviceSubIdMapper.get(slapType));
	}

	public int deviceStatus() {
		return 0;

	}

}
