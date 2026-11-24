# Anexo técnico de endpoints

Con el Gateway real disponible y Swagger operativo:

```bash
python3 -m pip install openpyxl==3.1.5
python3 scripts/ep3/export-api-contracts.py --base http://localhost:8080
```

El script obtiene los contratos de los seis microservicios directamente desde /openapi/{servicio}, guarda JSON y genera un Excel filtrable. El parámetro --base acepta la URL HTTPS de AWS después del despliegue. No copia contraseñas, JWT ni registros de usuarios. La hoja Acceso describe reglas del modo estricto; los códigos de respuesta proceden del contrato Swagger, no de ejecución de cada operación. Se requiere revisión y capturas reales para el documento de integración del Anexo 2. La salida no se presenta como evidencia hasta que se ejecute contra el stack certificado.
