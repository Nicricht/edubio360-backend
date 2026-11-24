# Estado verificable de seguridad

La excepción temporal de Snyk para CVE-2026-92877 venció el 9 de noviembre de 2026. El 24 de noviembre se retiró la excepción del archivo .snyk; no se amplió la fecha, ni se desactivaron los escaneos. La política ahora exige cero excepciones activas. Los escaneos Snyk Open Source y Code conservan severidad alta como límite de bloqueo; el despliegue AWS comprueba de nuevo sin política de exclusión.

El aviso https://security.snyk.io/vuln/SNYK-JAVA-ORGGLASSFISHJAXB-20883142 todavía indica que no hay versión corregida publicada. El parche oficial se encuentra en https://github.com/eclipse-ee4j/jaxb-ri/pull/2013. La protección JAXP del contenedor se conserva como mitigación adicional, no como sustituto de la reparación. Se debe resolver y verificar el resultado de Snyk antes de publicar en la nube.

Los informes JaCoCo y las líneas sin cobertura se registran incluso si Maven falla; no se reduce el umbral. No se ha certificado una resolución de la vulnerabilidad.
