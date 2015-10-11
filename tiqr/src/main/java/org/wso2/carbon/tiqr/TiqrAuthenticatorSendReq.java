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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class TiqrAuthenticatorSendReq {
    /**
     * Prompt for a login and an OTP and check if they are OK.
     */
    public String authenticate(String url, String urlParameters, String formParameters) {
        String line;
        StringBuffer responseString = new StringBuffer();
        try {
            URL tiqrEP = new URL(url);
            //URLEncoder.encode(
            String encodedData = formParameters;

            HttpURLConnection connection = (HttpURLConnection) tiqrEP.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            writer.write(encodedData);
            writer.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = br.readLine()) != null) {
                responseString.append(line);
            }
            br.close();
            connection.disconnect();
        } catch (Exception e) {
            return "Failed" + e.getMessage();
        }
        String rs = responseString.toString();
        return rs;
    }
}