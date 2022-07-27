# recia-uPortal-adds
Complementary classes for uPortal 5.2.x - this add class to use as beans into uPortal

- bean for jsp invoker, to be able to get the current focused portlet
- Servlet to be able to update user properties
- Servlet monitoring of current sessions, jvm stats
- Servlet and tools (event listener) for the StatsRecorder
- bean for jsp invoker, to be able to process user attributes, mainly for xiti init.
- bean for custom Guest username selector : feature of uPortal to use multi guest depending on serverName.
- bean to customize CAS login URL (Adding a token to avoid direct auth on CAS without passing by the portal)
- beans of a custom LdapPersonAttributeDao of the person-directory lib, to be able to customize LDAP Person Attributes by adding new attributes computed from others