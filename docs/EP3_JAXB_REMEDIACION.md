# Candidato a remediación JAXB

CVE-2026-92877 afecta a org.glassfish.jaxb:jaxb-core. Snyk no indica una versión publicada que lo corrija. Los caminos detectados en este reactor son Eureka Server y Hibernate vía Spring Data JPA.

Esta rama experimental sustituye la implementación GlassFish JAXB por EclipseLink MOXy 4.0.10 (implementación alternativa de Jakarta XML Binding), sin cambiar Hibernate/JPA ni Eureka. Se excluye el runtime vulnerable y Maven Enforcer falla si vuelve a aparecer cualquiera de los dos artefactos GlassFish. Se conserva la protección JAXP existente. Se añade prueba de proveedor real y roundtrip XML. No se añade una excepción Snyk.

No incorporar a la rama integral hasta aprobar compilación, pruebas Maven, Snyk, arranque de Eureka, persistencia MySQL y flujo completo Swarm. Referencias: https://security.snyk.io/vuln/SNYK-JAVA-ORGGLASSFISHJAXB-20883142 y https://eclipse.dev/eclipselink/documentation/4.0/moxy/moxy.html.
