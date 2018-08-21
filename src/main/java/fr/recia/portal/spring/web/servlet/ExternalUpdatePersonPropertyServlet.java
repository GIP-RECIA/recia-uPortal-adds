package fr.recia.portal.spring.web.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apereo.portal.security.IPerson;
import org.apereo.portal.security.IPersonManager;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.apereo.services.persondir.IPersonAttributes;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Une servlet qui met a jour un attribut utilisateur dans sa session .
 * Il n'y a pas de modification en bases.
 *
 * @author P. Legay
 *
 */
@Controller
public class ExternalUpdatePersonPropertyServlet implements InitializingBean {

    private static final String PARAM_ATTR_NAME = "name";
    private static final String PARAM_VALUE_NAME = "value";

    private enum TypeValueAuth {
        /* La valeur à appliquer doit être passée en paramètre */
        EXTERN,
        /* La valeur passée en paramètre n'est pas prise en compte , on prend la valeur intern a la DAO */
        INTERN,
        /* La valeur de la DAO (intern) doit être égale à la valeur passée (extern) */
        EQUALS,
        /* On prend la valeur externe si elle est non null et la valeur interne sinon */
        CHOICE,
        /* Idem que CHOICE sauf que l'on ne reload jamais les attributs de la DAO */
        NORELOAD;

        public boolean externRequired() {
            return this == EXTERN || this == EQUALS;
        }

        public boolean internRequired(){
            return this == INTERN || this == EQUALS;
        }

        public boolean noReload(){
            return this == NORELOAD;
        }
    }

    private class UpdatePropertyException extends NullPointerException {

		private static final long serialVersionUID = 1L;

		private int errorCode = 0;

	public UpdatePropertyException(String message, int errorCode) {
            super(message);
            this.errorCode= errorCode;
	}

        public int getErrorCode() {
            return errorCode;
        }
    }


    private IPersonManager personManager;

    private IPersonAttributeDao personAttributeDao;

    //avec @Autowired @Qualifier("map") il faut type avec HashMap ...

    @Resource(name="mapForExternalUpdatePersonProperty")
    private Map<String, String> attributsModifiables;

    private Map<String, TypeValueAuth> attributsType = new HashMap<>();

    public void setAttributsModifiables(Map<String, String> attributsModifiables) {
        this.attributsModifiables = attributsModifiables;
    }

    @Autowired
    public void setPersonManager(IPersonManager personManager) {
        this.personManager = personManager;
    }

    public IPersonManager getPersonManager() {
        return personManager;
    }

    public IPersonAttributeDao getPersonAttributeDao() {
        return personAttributeDao;
    }

    @Autowired
    @Required
    public void setPersonAttributeDao(@Value("#{requestAttributeMergingDao}")IPersonAttributeDao personAttributeDao) {
        if (personAttributeDao == null) {
            throw new NullPointerException("personnAttributeDao can not be null!");
        }
        this.personAttributeDao = personAttributeDao;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (attributsModifiables != null) {
            for (Entry<String, String> entry : attributsModifiables.entrySet()) {
                String attribut = "" ;
                TypeValueAuth type ;
                try {
                    attribut = notNull(entry.getKey(), "attribut null");
                    type = TypeValueAuth.valueOf(entry.getValue());
                } catch (Exception e) {
                    type = null;
                }
                attributsType.put(attribut, type);
            }
        }
    }

    private <T> T notNull(T object, String message) throws UpdatePropertyException {
        return notNull(object, message, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private <T> T notNull(T object, String message, int errorCode) throws UpdatePropertyException {
        if (object == null) {
            throw new UpdatePropertyException(message, errorCode);
        }
        if (object instanceof String) {

            if (StringUtils.isEmpty(object)) {
                throw new UpdatePropertyException(message, errorCode);
            }
        }
        return object;
    }

    @RequestMapping(value ="/updatepersonproperty", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void handleRequest(HttpServletRequest request, HttpServletResponse reponse) throws ServletException, IOException {
        try {
            final String attributName = notNull(request.getParameter(PARAM_ATTR_NAME), "parameter null : " + PARAM_ATTR_NAME, HttpServletResponse.SC_BAD_REQUEST);
            final String externValue = request.getParameter(PARAM_VALUE_NAME);
            String internValue=null;
            String value=null;

            TypeValueAuth type = notNull(attributsType.get(attributName), "attribute unmodifiable: " + attributName, HttpServletResponse.SC_NOT_ACCEPTABLE);

            if (type.externRequired()) {
                notNull(externValue, "parameter null : " + PARAM_VALUE_NAME, HttpServletResponse.SC_NOT_ACCEPTABLE);
            }

            final IPerson person = notNull(notNull(personManager, "personneManagerNull").getPerson(request), "person null");
            if (! person.isGuest()) {

                if (type.internRequired() || externValue == null) {

                    if (type.noReload()) {
                        internValue = (String) person.getAttribute(attributName);
                    } else {
                        IPersonAttributes personAttributes =
                                notNull(
                                        notNull(personAttributeDao, "personAttributeDao Null").getPerson(person.getName()),
                                        "personAttributes null");

                        internValue = (String) personAttributes.getAttributeValue(attributName);
                    }
                }

                switch (type) {
                    case EXTERN:
                        value = externValue;
                        break;
                    case INTERN:
                        value = internValue;
                        break;
                    case EQUALS:
                        value = externValue.equals(internValue) ? externValue : null;
                        break;
                    case CHOICE:
                    case NORELOAD:
                        value = externValue != null ? externValue : internValue;
                }
// À revoir cela car on doit spécifier un HTTPStatus OK ou le code erreur, et en passant un objet celui-ci est automatiquement traduit en json
                if (value != null) {
                    person.setAttribute(attributName, value);
                    reponse.getWriter().write(String.format("{\"%s\" : \"%s\"}", attributName,  value));
                } else {
                    reponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "final null value");
                }
            } else {
                reponse.sendError(HttpServletResponse.SC_FORBIDDEN, "guest");
            }
        } catch (UpdatePropertyException e) {
            reponse.sendError(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            reponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}