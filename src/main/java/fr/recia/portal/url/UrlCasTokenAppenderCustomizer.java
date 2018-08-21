/**
 * Copyright © 2018 GIP-RECIA (https://www.recia.fr/)
 * Copyright © 2018 Esup Portail https://www.esup-portail.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.recia.portal.url;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apereo.portal.url.IAuthUrlCustomizer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Customizer to append CAS toekn params to url.
 */
public class UrlCasTokenAppenderCustomizer implements IAuthUrlCustomizer, InitializingBean {

    /** Logger. */
    private static final Log LOG = LogFactory.getLog(UrlCasTokenAppenderCustomizer.class);

    /** Regex condition to apply Customization. */
    private String applyCondition = ".*login\\?service=https?://.*";

    /** Param name with equal and value to append to url. */
    private String paramToAppend = "token=";

    public void setApplyCondition(final String applyCondition) {
        this.applyCondition = applyCondition;
    }

    public void setParamToAppend(final String paramToAppend) {
        this.paramToAppend = paramToAppend;
    }


    public boolean supports(final HttpServletRequest request, final String url) {
        return url != null && url.matches(applyCondition);
    }

    public String customizeUrl(final HttpServletRequest request, final String url) {
        if (url != null && !url.isEmpty() && supports(request, url)) {
            final String token = this.generateToken();
            if (StringUtils.hasText(token)) {
                final String updatedUrl = url + "&" + paramToAppend + token;

                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Modifying Url Domain from [%s] to [%s]", url, updatedUrl));
                }
                return updatedUrl;
            }
        }
        return url;
    }

    /**
     * Generate the token depending of the time.
     *
     * @return the generated token
     */
    protected String generateToken() {
        String strToken = null;

        try {
            final MessageDigest md5Digester = MessageDigest.getInstance("MD5");
            // Le token change tous les jours. (Décalage de minuit => 4h du matin)
            final Long currentPeriod = (System.currentTimeMillis() + (1000*3600*4)) / (1000*3600*24);

            byte[] token = md5Digester.digest(String.valueOf(currentPeriod).getBytes());
            final StringBuilder sb = new StringBuilder(64);
            if ((token != null) && (token.length > 0)) {
                for (byte b : token) {
                    final String hex = Integer.toHexString(b);
                    if (hex.length() == 1) {
                        sb.append("0");
                        sb.append(hex);
                    } else {
                        sb.append(hex.substring(hex.length() - 2));
                    }
                }
            } else {
                LOG.error("Unable to generate the token !");
            }

            strToken = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Error while attempting to hash token with MD5 !", e);
        }

        return strToken;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.hasText(this.applyCondition, "No applyCondition supplied !");
        Assert.hasText(this.paramToAppend, "No paramToAppend supplied !");
    }
}
