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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.common.model.Property;

public class TiqrAuthenticator extends AbstractApplicationAuthenticator implements FederatedApplicationAuthenticator {

    private static Log log = LogFactory.getLog(TiqrAuthenticator.class);
    private String enrolUserBody = null;

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
        if (request.getParameter(TiqrConstants.ENROLL_USERID) != null
                && request.getParameter(TiqrConstants.ENROLL_DISPLAYNAME) != null) {
            return true;
        }
        return false;
    }

    /**
     * initiate the authentication request
     */
    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {
        try {
            Map<String, String> authenticatorProperties = context
                    .getAuthenticatorProperties();
            if (authenticatorProperties != null) {
                enrolUserBody = enrolUser(authenticatorProperties);
                response.sendRedirect(enrolUserBody.substring(enrolUserBody.indexOf("https"), enrolUserBody.indexOf("'/>")));
                log.info("The QR code is successfully displayed.");
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Error while retrieving properties. Authenticator Properties cannot be null");
                }
                throw new AuthenticationFailedException(
                        "Error while retrieving properties. Authenticator Properties cannot be null");
            }
        } catch (Exception e) {
            log.error("Exception while showing the QR code", e);
            throw new AuthenticationFailedException(e.getMessage(), e);
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
     * this method is overridden for extra claim request to Tiqr end-point
     */
    @Override
    protected void processAuthenticationResponse(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context) throws AuthenticationFailedException {
        try {
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
            String formParameters = "uid=" + userId + "&displayName=" + diaplayName;
            TiqrAuthenticatorSendReq auth = new TiqrAuthenticatorSendReq();
            String result = auth.authenticate(urlToEntrol, "", formParameters);
            String qrCode = result.substring(result.indexOf("<img"), result.indexOf("</body>"));
            return qrCode;
        }
        return null;
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