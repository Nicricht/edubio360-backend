# Renovación de sesión JWT (candidato de integración)

La pauta DSY1104 EP3, página 3, exige generación, verificación y renovación segura de JWT. Se añade POST /api/auth/refresh, protegido por el Gateway y verificación de firma en Auth. Un JWT vigente permite renovar su acceso por 15 minutos sin superar el límite absoluto de ocho horas desde el login. Se exige cuenta activa y coincidencia del correo y rol actuales. Un token vencido o manipulado no se renueva. El nuevo token tiene identificador independiente.

No se crea un refresh token de larga duración ni se guardan contraseñas en el cliente. El usuario debe iniciar sesión nuevamente si vence el acceso antes de renovarse o termina el límite absoluto. Los tokens de acceso ya emitidos siguen siendo válidos hasta su expiración, salvo controles adicionales; no se declara revocación instantánea de todos los tokens. Pendiente comprobar los cambios en CI y unirlos con el frontend antes de la entrega.
