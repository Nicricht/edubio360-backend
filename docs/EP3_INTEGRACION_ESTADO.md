# Integración de repositorios

Backend y frontend se mantienen separados. El pipeline backend fija el commit frontend ac68fd6dac5ca8c43d49f8b7d868defe85a30821 para reproducibilidad, construye ambas imágenes y levanta el stack de Swarm. Nginx entrega React y proxy REST privado. El ALB interno utiliza rutas para separar API y frontend; AWS API Gateway entrega HTTPS. No se habilita el despliegue cloud si el escaneo de seguridad falla. La prueba Chromium comprueba login, recarga, envío y recuperación de solicitud y bloqueo de interfaz de administración. Las capturas serán evidencia solo si la prueba pasa.

Aún se debe validar el stack en ejecución, la autorización efectiva del laboratorio, SQS/Lambda, observabilidad completa y los entregables académicos. No se han creado recursos AWS en este incremento.
