/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.tiqr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.amber.oauth2.client.request.OAuthClientRequest;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.ui.CarbonUIUtil;

public class TiqrAuthenticator extends AbstractApplicationAuthenticator implements FederatedApplicationAuthenticator {

    private static Log log = LogFactory.getLog(TiqrAuthenticator.class);
    private String enrolUserBody = null;
    private String qrCode = null;
    private String sessionId = null;
    private boolean isCompleted = false;
    /**
     * @return
     */
    protected String getTiqrEndpoint(
            Map<String, String> authenticatorProperties) {

        return "http://" + authenticatorProperties.get(TiqrConstants.TIQR_CLIENTIP)
                + ":8080";
    }

    /**
     * Check whether the authentication or logout request can be handled by the authenticator
     */
    @Override
    public boolean canHandle(HttpServletRequest request) {
        if (log.isTraceEnabled()) {
            log.trace("Inside TiqrAuthenticator.canHandle()");
        }
        try {
            if(isCompleted) {qrCode = null;}
            return (qrCode != null && qrCode.startsWith("<img alt=\"QR\""));
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Initiate the authentication request
     */
    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {
        try {
            isCompleted = false;
            if(enrolUserBody == null) {
                Map<String, String> authenticatorProperties = context
                        .getAuthenticatorProperties();
                if (authenticatorProperties != null) {
                    enrolUserBody = enrolUser(authenticatorProperties);
                    response.sendRedirect(enrolUserBody.substring(enrolUserBody.indexOf("https"), enrolUserBody.indexOf("'/>")));
                    log.info("The QR code is successfully displayed.");
//                sendRESTCall("http://localhost:8080/travelocity.com/samlsso", "", "", "GET");
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Error while retrieving properties. Authenticator Properties cannot be null");
                    }
                    throw new AuthenticationFailedException(
                            "Error while retrieving properties. Authenticator Properties cannot be null");
                }
            }
        } catch (IOException e) {
            throw new AuthenticationFailedException("Exception while showing the QR code: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new AuthenticationFailedException("Exception while showing the QR code: " + e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
            throw new AuthenticationFailedException("Unable to get QR code: " + e.getMessage(), e);
        }
        return;
    }

    /**
     * Get the configuration properties of UI
     */
    @Override
    public List<Property> getConfigurationProperties() {
        List<Property> configProperties = new ArrayList<Property>();

        Property userId = new Property();
        userId.setName(TiqrConstants.ENROLL_USERID);
        userId.setDisplayName("User Id");
        userId.setRequired(true);
        userId.setDescription("Enter user identifier to entroll the user in Tiqr");
        configProperties.add(userId);

        Property displayName = new Property();
        displayName.setName(TiqrConstants.ENROLL_DISPLAYNAME);
        displayName.setDisplayName("Display Name");
        displayName.setRequired(true);
        displayName.setDescription("Enter user's display name to entrol the user in Tiqr");
        configProperties.add(displayName);

        Property clientIP = new Property();
        clientIP.setName(TiqrConstants.TIQR_CLIENTIP);
        clientIP.setDisplayName("clientIP");
        clientIP.setRequired(true);
        clientIP.setDescription("Enter the IP address of the tiqr client");
        configProperties.add(clientIP);
        return configProperties;
    }

    /**
     * This method is overridden for extra claim request to Tiqr end-point
     */
    @Override
    protected void processAuthenticationResponse(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context) throws AuthenticationFailedException {
        try {
            Map<String, String> authenticatorProperties = context
                    .getAuthenticatorProperties();
            String tiqrEP = getTiqrEndpoint(authenticatorProperties);
            if (tiqrEP == null) {
                tiqrEP = "http://" + authenticatorProperties.get(TiqrConstants.TIQR_CLIENTIP)
                        + ":8080";
            }
            String urlToCheckEntrolment = tiqrEP + "/enrol.php";
            int status = 0;
            int iteration = 0;
            while (true) {
                try {
                    String res = sendRESTCall(urlToCheckEntrolment, "", "action=getStatus&sessId=" + sessionId, "POST");
                    status = Integer.parseInt(res.substring(res.indexOf("Enrolment status: "), res.indexOf("<!DOCTYPE")).replace("Enrolment status: ","").trim());
                    log.info("Enrolment status: "+status);
                    if (status == 5) {
                        log.info("Successfully enrolled the user with User ID:"
                                + authenticatorProperties.get(TiqrConstants.ENROLL_USERID)
                                + "and Display Name:" + authenticatorProperties.get(TiqrConstants.ENROLL_DISPLAYNAME));
                        break;
                    }
                    log.info("Enrolment pending...");
                    Thread.sleep(10000);
                    iteration++;
                    if (iteration == 11) {
                        log.warn("Enrolment timed out.");
                        break;
                    }
                } catch (InterruptedException e) {
                    throw new AuthenticationFailedException(
                            "Interruption occured while getting the enrolment status" + e.getMessage(), e);
                } catch (NumberFormatException e) {
                    throw new AuthenticationFailedException("Error while getting the enrolment status"
                            + e.getMessage(), e);
                } catch (IndexOutOfBoundsException e) {
                    throw new AuthenticationFailedException("Error while getting the enrolment status"
                            + e.getMessage(), e);
                }
            }
            if (status == 5) {
                context.setSubject("Successfully enrolled the user");
                log.info("Successfully enrolled the user");
            } else {
                context.setSubject("Enrolment process is failed");
                throw new AuthenticationFailedException("Enrolment process is Failed");
            }
            isCompleted = true;
            qrCode = null;
        }
        catch (Exception e) {
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
    }

    public String enrolUser(Map<String, String> authenticatorProperties) {
        String tiqrEP = getTiqrEndpoint(authenticatorProperties);
        if (tiqrEP == null) {
            tiqrEP = "http://" + authenticatorProperties.get(TiqrConstants.TIQR_CLIENTIP)
                    + ":8080";
        }
        String urlToEntrol = tiqrEP + "/enrol.php";
        String userId = authenticatorProperties
                .get(TiqrConstants.ENROLL_USERID);
        String diaplayName = authenticatorProperties
                .get(TiqrConstants.ENROLL_DISPLAYNAME);
        if (userId != null && diaplayName != null) {
            String formParameters = "uid=" + userId + System.currentTimeMillis() + "&displayName=" + diaplayName;
            String result = sendRESTCall(urlToEntrol, "", formParameters, "POST");
            try {
                sessionId = result.substring(result.indexOf("Session id: ["), result.indexOf("<img")).replace("Session id: [", "").replace("]","").trim();
                qrCode = result.substring(result.indexOf("<img"), result.indexOf("</body>"));
                return qrCode;
            } catch (NullPointerException e) {
                log.error("Unable to find QR code" + e.getMessage());
                return null;
            } catch (IndexOutOfBoundsException e) {
                log.error("Error while getting the QR code" + e.getMessage());
                return null;
            }
        }
        return null;
    }

    public String sendRESTCall(String url, String urlParameters, String formParameters, String httpMethod) {
        String line;
        StringBuffer responseString = new StringBuffer();
        try {
            URL tiqrEP = new URL(url + urlParameters);

            String encodedData = formParameters;

            HttpURLConnection connection = (HttpURLConnection) tiqrEP.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod(httpMethod);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if(httpMethod.toUpperCase().equals("POST")) {
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
                writer.write(encodedData);
                writer.close();
            }
            if(connection.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((line = br.readLine()) != null) {
                    responseString.append(line);
                }
                br.close();
            }
            connection.disconnect();
        } catch (Exception e) {
            return "Failed" + e.getMessage();
        }
        String rs = responseString.toString();
        return rs;
    }

    /**
     * Get the friendly name of the Authenticator
     */
    @Override
    public String getFriendlyName() {
        return TiqrConstants.TIQR_FRIENDLY_NAME;
    }

    /**
     * Get the name of the Authenticator
     */
    @Override
    public String getName() {
        return TiqrConstants.TIQR_FRIENDLY_NAME;
    }

    public String getContextIdentifier(HttpServletRequest httpServletRequest) {
        return null;
    }
}