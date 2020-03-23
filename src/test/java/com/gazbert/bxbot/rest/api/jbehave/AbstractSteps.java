/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 gazbert
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gazbert.bxbot.rest.api.jbehave;

import static junit.framework.TestCase.assertEquals;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gazbert.bxbot.rest.api.domain.JwtAuthenticationRequest;
import com.gazbert.bxbot.rest.api.domain.JwtAuthenticationResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jbehave.core.annotations.BeforeStories;
import org.jbehave.core.annotations.Then;

/**
 * Base class for all JBehave test steps.
 *
 * @author gazbert
 */
public abstract class AbstractSteps {

  private static final Logger LOG = LogManager.getLogger();

  private String adminUsername;
  private String adminPassword;
  private String userUsername;
  private String userPassword;
  private String baseApiPath;

  private enum UserType {
    USER,
    ADMIN
  }

  private String apiPath;
  private HttpResponse httpResponse;

  @BeforeStories
  public void loadConfiguration() throws Exception {

    final Properties props = new Properties();
    final InputStream is = AbstractSteps.class.getClassLoader()
        .getResourceAsStream("restapi-config.properties");
    assert is != null;
    props.load(is);

    baseApiPath = props.getProperty("bxbot.restapi.baseuri");
    LOG.info(() -> "[CONFIG] Base API path: " + baseApiPath);

    adminUsername = props.getProperty("bxbot.restapi.admin.username");
    LOG.info(() -> "[CONFIG] Admin username: " + adminUsername);

    adminPassword = props.getProperty("bxbot.restapi.admin.password");
    LOG.info(() -> "[CONFIG] Admin password: " + adminPassword);

    userUsername = props.getProperty("bxbot.restapi.user.username");
    LOG.info(() -> "[CONFIG] User username: " + userUsername);

    userPassword = props.getProperty("bxbot.restapi.user.password");
    LOG.info(() -> "[CONFIG] User password: " + userPassword);
  }

  // --------------------------------------------------------------------------
  // Common candidate steps
  // --------------------------------------------------------------------------

  @Then("the bot will respond with 401 Unauthorized")
  public void thenBotRespondsWith401Unauthorized() {
    assertEquals(SC_UNAUTHORIZED, httpResponse.getStatusLine().getStatusCode());
  }

  @Then("the bot will respond with 404 Not Found")
  public void thenBotRespondsWith404NotFound() {
    assertEquals(SC_NOT_FOUND, httpResponse.getStatusLine().getStatusCode());
  }

  @Then("the bot will respond with 403 Forbidden")
  public void thenBotRespondsWith403Forbidden() {
    assertEquals(SC_FORBIDDEN, httpResponse.getStatusLine().getStatusCode());
  }

  // --------------------------------------------------------------------------
  // Accessors for domain resources
  // --------------------------------------------------------------------------

  protected String getBaseApiPath() {
    return baseApiPath;
  }

  protected String getApiPath() {
    return apiPath;
  }

  protected void setApiPath(String apiPath) {
    this.apiPath = apiPath;
  }

  protected HttpResponse getHttpResponse() {
    return httpResponse;
  }

  protected void setHttpResponse(HttpResponse httpResponse) {
    this.httpResponse = httpResponse;
  }

  // --------------------------------------------------------------------------
  // Convenience methods for subclasses to call
  // --------------------------------------------------------------------------

  protected String getUserToken() throws IOException {
    return getToken(UserType.USER);
  }

  protected String getAdminToken() throws IOException {
    return getToken(UserType.ADMIN);
  }

  protected HttpResponse makeGetApiCallWithToken(String apiPath, String token) throws IOException {
    return makeGetApiCall(apiPath, token);
  }

  protected HttpResponse makeGetApiCallWithoutToken(String apiPath) throws IOException {
    return makeGetApiCall(apiPath, null);
  }

  protected HttpResponse makeUpdateApiCallWithToken(String apiPath, String token, String payload)
      throws IOException {
    return makeUpdateApiCall(apiPath, token, payload);
  }

  protected HttpResponse makeUpdateApiCallWithoutToken(String apiPath, String payload)
      throws IOException {
    return makeUpdateApiCall(apiPath, null, payload);
  }

  // --------------------------------------------------------------------------
  // Private utils
  // --------------------------------------------------------------------------

  private String getToken(UserType userType) throws IOException {

    JwtAuthenticationRequest jwtAuthenticationRequest = null;
    if (userType == UserType.USER) {
      jwtAuthenticationRequest = new JwtAuthenticationRequest(userUsername, userPassword);
    } else if (userType == UserType.ADMIN) {
      jwtAuthenticationRequest = new JwtAuthenticationRequest(adminUsername, adminPassword);
    }

    final ObjectMapper mapper = new ObjectMapper();
    final String credentialsJson = mapper.writeValueAsString(jwtAuthenticationRequest);
    final StringEntity requestEntity =
        new StringEntity(credentialsJson, ContentType.APPLICATION_JSON);
    final HttpPost postMethod = new HttpPost(baseApiPath + "/token");
    postMethod.setEntity(requestEntity);

    final CloseableHttpClient httpclient = HttpClients.createDefault();
    final HttpResponse response = httpclient.execute(postMethod);

    final HttpEntity responseEntity = response.getEntity();
    final Header encodingHeader = responseEntity.getContentEncoding();

    final Charset encoding =
        encodingHeader == null
            ? StandardCharsets.UTF_8
            : Charsets.toCharset(encodingHeader.getValue());

    final String responseJson = EntityUtils.toString(responseEntity, encoding);
    final JwtAuthenticationResponse authenticationResponse =
        mapper.readValue(responseJson, JwtAuthenticationResponse.class);

    httpclient.close();

    return authenticationResponse.getToken();
  }

  private HttpResponse makeGetApiCall(String api, String token) throws IOException {
    final HttpUriRequest request = new HttpGet(api);
    if (token != null) {
      request.setHeader("Authorization", "Bearer " + token);
    }
    return HttpClientBuilder.create().build().execute(request);
  }

  private HttpResponse makeUpdateApiCall(String api, String token, String payload)
      throws IOException {
    final HttpPut request = new HttpPut(api);
    if (token != null) {
      request.setHeader("Authorization", "Bearer " + token);
    }
    final StringEntity requestEntity = new StringEntity(payload, ContentType.APPLICATION_JSON);
    request.setEntity(requestEntity);
    request.setHeader("Authorization", "Bearer " + token);
    return HttpClientBuilder.create().build().execute(request);
  }
}
