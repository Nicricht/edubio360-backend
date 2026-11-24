# Estado verificable de seguridad

La excepción temporal de Snyk para CVE-2026-92877 venció el 9 de noviembre de 2026. El 24 de noviembre se retiró la excepción del archivo .snyk; no se amplió la fecha, ni se desactivaron los escaneos. La política ahora exige cero excepciones activas. Los escaneos Snyk Open Source y Code conservan severidad alta como límite de bloqueo; el despliegue AWS comprueba de nuevo sin política de exclusión.

El aviso https://security.snyk.io/vuln/SNYK-JAVA-ORGGLASSFISHJAXB-20883142 todavía indica que no hay versión corregida publicada. El parche oficial se encuentra en https://github.com/eclipse-ee4j/jaxb-ri/pull/2013. La protección JAXP del contenedor se conserva como mitigación adicional, no como sustituto de la reparación. Se debe resolver y verificar el resultado de Snyk antes de publicar en la nube.

Los informes JaCoCo y las líneas sin cobertura se registran incluso si Maven falla; no se reduce el umbral. No se ha certificado una resolución de la vulnerabilidad.

El escaneo de 24-11-2026 también detectó vulnerabilidades altas y críticas en Spring Framework, Spring Boot, Spring Security, Spring AMQP, Spring Cloud Gateway y Jackson. Se actualiza el BOM a Spring Boot 4.0.10 y Spring Cloud 2025.1.5, publicados el 20-11-2026. Boot gestiona Jackson 2.21.8 y 3.1.8, Framework 7.0.11, Security 7.0.9 y AMQP 4.0.7; Cloud gestiona Gateway 5.0.5. Pendiente reescaneo. No se declara que JAXB esté resuelto. Referencias https://spring.io/blog/2026/11/20/spring-boot-4-0-10-available-now/ y https://spring.io/blog/2026/11/20/spring-cloud-2025-1-5-has-been-released/.
