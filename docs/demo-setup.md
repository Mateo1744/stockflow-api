# Datos de demostración

Este mecanismo es explícito, usa únicamente la API y no modifica las migraciones de producción. Requiere una API activa y el administrador creado mediante bootstrap.

```powershell
$credential = Get-Credential -UserName admin@example.com
.\docs\demo-data.ps1 -AdminCredential $credential
```

La contraseña se solicita de forma interactiva y no se guarda. Cada ejecución utiliza identificadores únicos, crea tres categorías, dos proveedores, tres clientes y seis productos. Después confirma una entrada de 20 unidades, completa una venta de 3 y confirma una salida de 2. El script consulta inventario, movimientos y reportes y falla si el stock final no es 15.

Los demás productos conservan stock cero y aparecen en low-stock conforme a la regla real de la API. Los datos demo pueden identificarse mediante el prefijo `DEMO-`.
