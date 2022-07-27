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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.directory.SearchControls;

import org.apache.commons.lang3.StringUtils;
import org.apereo.services.persondir.IPersonAttributes;
import org.apereo.services.persondir.support.AbstractQueryPersonAttributeDao;
import org.apereo.services.persondir.support.CaseInsensitiveAttributeNamedPersonImpl;
import org.apereo.services.persondir.support.CaseInsensitiveNamedPersonImpl;
import org.apereo.services.persondir.support.QueryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.filter.LikeFilter;
import org.springframework.util.Assert;

public class LdapPersonAttributeDao extends AbstractQueryPersonAttributeDao<LogicalFilterWrapper> implements InitializingBean {

    private static final Pattern QUERY_PLACEHOLDER = Pattern.compile("\\{0\\}");
    private static final AttributesMapper MAPPER = new AttributeMapAttributesMapper();
    private LdapTemplate ldapTemplate = null;
    private String baseDN = "";
    private String queryTemplate = null;
    private ContextSource contextSource = null;
    private SearchControls searchControls = new SearchControls();
    private final boolean setReturningAttributes = true;
    private QueryType queryType;

    private List<AttributeFormatter> formatters;

    public LdapPersonAttributeDao() {
        this.queryType = QueryType.AND;
        this.searchControls.setSearchScope(2);
        this.searchControls.setReturningObjFlag(false);
    }

    public void afterPropertiesSet() throws Exception {
        Map<String, Set<String>> resultAttributeMapping = this.getResultAttributeMapping();
        this.getClass();
        if (resultAttributeMapping != null) {
            this.searchControls.setReturningAttributes((String[])resultAttributeMapping.keySet().toArray(new String[resultAttributeMapping.size()]));
        }

        if (this.contextSource == null) {
            throw new BeanCreationException("contextSource must be set");
        }

        if (this.formatters == null || this.formatters.isEmpty()) {
            throw new BeanCreationException("formatters must be set");
        }
    }

    protected LogicalFilterWrapper appendAttributeToQuery(LogicalFilterWrapper queryBuilder, String dataAttribute, List<Object> queryValues) {
        if (queryBuilder == null) {
            queryBuilder = new LogicalFilterWrapper(this.queryType);
        }

        for(Object queryValue: queryValues) {
            String queryValueString = queryValue == null ? null : queryValue.toString();
            if (StringUtils.isNotBlank(queryValueString)) {
                Filter filter;
                if (!queryValueString.contains("*")) {
                    filter = new EqualsFilter(dataAttribute, queryValueString);
                } else {
                    filter = new LikeFilter(dataAttribute, queryValueString);
                }

                queryBuilder.append(filter);
            }
        }

        return queryBuilder;
    }

    protected List<IPersonAttributes> getPeopleForQuery(LogicalFilterWrapper queryBuilder, String queryUserName) {
        String generatedLdapQuery = queryBuilder.encode();
        if (StringUtils.isBlank(generatedLdapQuery)) {
            return null;
        } else {
            String ldapQuery;
            if (this.queryTemplate == null) {
                ldapQuery = generatedLdapQuery;
            } else {
                Matcher queryMatcher = QUERY_PLACEHOLDER.matcher(this.queryTemplate);
                ldapQuery = queryMatcher.replaceAll(generatedLdapQuery);
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Final ldapQuery after applying queryTemplate: '" + ldapQuery + "'");
                }
            }

            List<Map<String, List<Object>>> unformattedqueryResults = this.ldapTemplate.search(this.baseDN, ldapQuery, this.searchControls, MAPPER);

            //apply formatters
            List<Map<String, List<Object>>> queryResults = new ArrayList<>(unformattedqueryResults.size());
            for (Map<String, List<Object>> personAttrs: unformattedqueryResults) {
                Map<String, List<Object>> newPersonAttrs = personAttrs;
                for (AttributeFormatter formatter: this.formatters){
                    newPersonAttrs = formatter.apply(newPersonAttrs);
                }
                queryResults.add(newPersonAttrs);
            }

            List<IPersonAttributes> peopleAttributes = new ArrayList(queryResults.size());

            IPersonAttributes person;
            for(Map<String, List<Object>> queryResult: queryResults) {
                String userNameAttribute = this.getConfiguredUserNameAttribute();
                if (this.isUserNameAttributeConfigured() && queryResult.containsKey(userNameAttribute)) {
                    person = new CaseInsensitiveAttributeNamedPersonImpl(userNameAttribute, queryResult);
                } else if (queryUserName != null) {
                    person = new CaseInsensitiveNamedPersonImpl(queryUserName, queryResult);
                } else {
                    person = new CaseInsensitiveAttributeNamedPersonImpl(userNameAttribute, queryResult);
                }
                peopleAttributes.add(person);
            }

            return peopleAttributes;
        }
    }

    public static class AttributeFormatter {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private String attributeName;
        private String attributeSource;
        private Pattern patternMatcher;

        public AttributeFormatter(String attributeName, String attributeSource, String patternMatcher) {
            this.attributeName = attributeName;
            this.attributeSource = attributeSource;
            this.patternMatcher = Pattern.compile(patternMatcher);
        }

        public Map<String, List<Object>> apply(Map<String, List<Object>> personAttributes){
            if ( personAttributes != null && !personAttributes.isEmpty()) {
                Map<String, List<Object>> newAttributes = new HashMap<>();
                for (Map.Entry<String, List<Object>> attributes: personAttributes.entrySet()) {
                    if (attributes.getKey().equalsIgnoreCase(attributeSource)) {
                        List<Object> matchedValues = new ArrayList<>();
                        for (Object value: attributes.getValue()) {
                            final Matcher matcher = (value instanceof String) ? patternMatcher.matcher((String) value) : null;
                            if (matcher != null  && matcher.find()) {
                                matchedValues.add(matcher.group(1));
                            }
                        }
                        if (!matchedValues.isEmpty()) {
                            logger.debug("Formatter add attribute [{}={}]", attributeName, matchedValues);
                            newAttributes.putIfAbsent(attributeName, matchedValues);
                        } else {
                            logger.warn("Formatter '{}' didn't return a value whereas it matched.", this);
                        }
                    }
                    newAttributes.put(attributes.getKey(), attributes.getValue());
                }
                return newAttributes;
            }
            return personAttributes;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", AttributeFormatter.class.getSimpleName() + "[", "]")
                    .add("attributeName='" + attributeName + "'")
                    .add("attributeSource='" + attributeSource + "'")
                    .add("patternMatcher=" + patternMatcher)
                    .toString();
        }
    }

    /** @deprecated */
    @Deprecated
    public int getTimeLimit() {
        return this.searchControls.getTimeLimit();
    }

    /** @deprecated */
    @Deprecated
    public void setTimeLimit(int ms) {
        this.searchControls.setTimeLimit(ms);
    }

    public String getBaseDN() {
        return this.baseDN;
    }

    public void setBaseDN(String baseDN) {
        if (baseDN == null) {
            baseDN = "";
        }

        this.baseDN = baseDN;
    }

    public ContextSource getContextSource() {
        return this.contextSource;
    }

    public synchronized void setContextSource(ContextSource contextSource) {
        Assert.notNull(contextSource, "contextSource can not be null");
        this.contextSource = contextSource;
        this.ldapTemplate = new LdapTemplate(this.contextSource);
    }

    public synchronized void setLdapTemplate(LdapTemplate ldapTemplate) {
        Assert.notNull(ldapTemplate, "ldapTemplate cannot be null");
        this.ldapTemplate = ldapTemplate;
        this.contextSource = this.ldapTemplate.getContextSource();
    }

    public SearchControls getSearchControls() {
        return this.searchControls;
    }

    public void setSearchControls(SearchControls searchControls) {
        Assert.notNull(searchControls, "searchControls can not be null");
        this.searchControls = searchControls;
    }

    public QueryType getQueryType() {
        return this.queryType;
    }

    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }

    public String getQueryTemplate() {
        return this.queryTemplate;
    }

    public void setQueryTemplate(String queryTemplate) {
        this.queryTemplate = queryTemplate;
    }

    public LdapTemplate getLdapTemplate() {
        return ldapTemplate;
    }

    public List<AttributeFormatter> getFormatters() {
        return formatters;
    }

    public void setFormatters(List<AttributeFormatter> formatters) {
        this.formatters = formatters;
    }
}