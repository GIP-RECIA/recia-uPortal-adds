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
package fr.recia.portal.persondir;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import org.springframework.ldap.core.AttributesMapper;

class AttributeMapAttributesMapper implements AttributesMapper {
    private final boolean ignoreNull;

    public AttributeMapAttributesMapper() {
        this(false);
    }

    public AttributeMapAttributesMapper(boolean ignoreNull) {
        this.ignoreNull = ignoreNull;
    }

    public Object mapFromAttributes(Attributes attributes) throws NamingException {
        int attributeCount = attributes.size();
        Map<String, Object> mapOfAttrValues = this.createAttributeMap(attributeCount);
        NamingEnumeration<? extends Attribute> attributesEnum = attributes.getAll();

        while(true) {
            Attribute attribute;
            do {
                if (!attributesEnum.hasMore()) {
                    return mapOfAttrValues;
                }

                attribute = (Attribute)attributesEnum.next();
            } while(this.ignoreNull && attribute.size() <= 0);

            String attrName = attribute.getID();
            String key = this.getAttributeKey(attrName);
            NamingEnumeration<?> valuesEnum = attribute.getAll();
            List<?> values = this.getAttributeValues(valuesEnum);
            mapOfAttrValues.put(key, values);
        }
    }

    protected Map<String, Object> createAttributeMap(int attributeCount) {
        return new TreeMap(String.CASE_INSENSITIVE_ORDER);
    }

    protected String getAttributeKey(String attributeName) {
        return attributeName;
    }

    protected List<?> getAttributeValues(NamingEnumeration<?> values) {
        return Collections.list(values);
    }
}