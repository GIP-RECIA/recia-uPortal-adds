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
package fr.recia.portal.portlets.jsp;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class TransformUserAttributes {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, String> regexMap;
    private Map<String, Pattern> patternMap;

    public Map<String, String> getRegexMap() {
        return regexMap;
    }

    public void setRegexMap(Map<String, String> regexMap) {
        this.regexMap = regexMap;
        if (regexMap != null) {
            patternMap = new HashMap<String, Pattern>(regexMap.size());
            for (Map.Entry<String , String> entry : regexMap.entrySet()) {
                patternMap.put(entry.getKey(), Pattern.compile(entry.getValue()));
            }
        } else {
            patternMap = null;
        }
    }

    public String processAttribute(String attributeName, Object values) {
        logger.debug("process attribute name '{}' and value(s) '{}'", attributeName, values);
        String processedVal = "";
        if (!StringUtils.isEmpty(attributeName) && values != null) {
            if (values instanceof Object[]) {
                StringBuilder sb = new StringBuilder();
                for (Object o : (Object[]) values) {
                    sb.append('\t');
                    sb.append(o.toString());
                }
                processedVal = sb.toString();
                logger.debug("value: {}  ", processedVal);
            } else {
                processedVal = values.toString();
            }
            // on parse les strings corespondant au pattern.
            if (patternMap != null) {
                Pattern p = patternMap.get(attributeName);
                if (p != null) {
                    Matcher m = p.matcher((String) processedVal);
                    if (m.matches()) {
                        processedVal = m.group(1);
                    }
                }
            }
        }
        return processedVal;
    }
}
