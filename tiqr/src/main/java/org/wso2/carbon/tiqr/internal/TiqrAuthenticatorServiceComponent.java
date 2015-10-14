package org.wso2.carbon.tiqr.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.tiqr.TiqrAuthenticator;

import java.util.Hashtable;

/**
 * @scr.component name="identity.application.authenticator.tiqr.component" immediate="true"
 */
public class TiqrAuthenticatorServiceComponent {

    private static Log log = LogFactory.getLog(TiqrAuthenticatorServiceComponent.class);

    protected void activate(ComponentContext ctxt) {
        try {
            TiqrAuthenticator tiqrAuthenticator = new TiqrAuthenticator();
            Hashtable<String, String> props = new Hashtable<String, String>();

            ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(),
                    tiqrAuthenticator, props);
            if (log.isDebugEnabled()) {
                log.debug("Tiqr authenticator is activated");
            }
        } catch (Throwable e) {
            log.fatal("Tiqr authenticator authenticator ", e);
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("Tiqr authenticator is deactivated");
        }
    }
}
