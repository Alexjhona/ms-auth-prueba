# OWASP ZAP

Ejecuta el escaneo DAST de este microservicio desde la carpeta del repo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\security\owasp-zap\run-zap.ps1
```

El servicio debe estar levantado y publicar `/v3/api-docs`.
