# Instrucciones de ejecución y pruebas — Bank XYZ EFT

Este documento explica cómo preparar, ejecutar y probar los componentes del proyecto **Bank XYZ EFT**.

> El procedimiento de despliegue en AWS se documenta por separado en `despliegue.md`.

---

## 1. Requisitos previos

Para ejecutar el proyecto se requiere:

- Docker Engine o Docker Desktop.
- Docker Compose v2.
- Git.
- Acceso válido a Oracle Autonomous Database.
- Oracle Wallet correspondiente a la base de datos.
- Archivo `.env` configurado.
- Keystores PKCS12 para los tres BFF.
- Java y Maven únicamente si se desean ejecutar los procesos Spring Batch directamente desde el host.

Los contenedores utilizan Java 21.

---

## 2. Obtener el proyecto

Repositorio oficial:

```text
https://github.com/PanshoOw/Bank-XYZ-EFT
```

Clonar:

```powershell
git clone https://github.com/PanshoOw/Bank-XYZ-EFT.git
cd Bank-XYZ-EFT
```

---

## 3. Configurar variables de entorno

El repositorio incluye:

```text
.env.example
```

Crear el archivo local:

```powershell
Copy-Item .env.example .env
```

El archivo `.env` real no debe almacenarse en Git.

Variables sensibles principales:

```text
BANKXYZ_DB_PASSWORD
ORACLE_WALLET_HOST_PATH

OAUTH_WEB_CLIENT_SECRET
OAUTH_MOBILE_CLIENT_SECRET
OAUTH_ATM_CLIENT_SECRET

BFF_WEB_SSL_KEYSTORE_PASSWORD
BFF_MOBILE_SSL_KEYSTORE_PASSWORD
BFF_ATM_SSL_KEYSTORE_PASSWORD
```

No deben versionarse:

```text
.env
Oracle Wallet
contraseñas
OAuth client secrets
tokens
keystore.p12
claves privadas
```

---

## 4. Configurar Oracle Wallet

El Oracle Wallet debe mantenerse fuera del repositorio.

La variable:

```text
ORACLE_WALLET_HOST_PATH
```

debe apuntar al directorio que contiene los archivos del Wallet.

Entre ellos:

```text
cwallet.sso
tnsnames.ora
sqlnet.ora
```

Los servicios que utilizan Oracle son:

```text
backend
cuentas-service
pagos-service
clientes-service
```

Docker Compose monta el Wallet dentro de los contenedores correspondientes.

---

## 5. Configurar HTTPS de los BFF

Los tres BFF requieren su archivo:

```text
keystore.p12
```

en:

```text
bff-web/src/main/resources/keystore.p12
bff-mobile/src/main/resources/keystore.p12
bff-atm/src/main/resources/keystore.p12
```

Las contraseñas se reciben mediante:

```text
BFF_WEB_SSL_KEYSTORE_PASSWORD
BFF_MOBILE_SSL_KEYSTORE_PASSWORD
BFF_ATM_SSL_KEYSTORE_PASSWORD
```

Los certificados utilizados durante desarrollo son autofirmados.

Por este motivo, los ejemplos con `curl` utilizan:

```text
-k
```

para permitir el certificado de desarrollo.

---

# 6. Datos de entrada para Spring Batch

Los datos legacy utilizados por los procesos batch provienen del repositorio indicado en el caso:

```text
https://github.com/KariVillagran/fin_legacy_data
```

Los archivos requeridos son:

```text
movimientos_financieros_diarios.csv
intereses_trimestrales.csv
estados_financieros_anuales.csv
```

Deben encontrarse en:

```text
src/main/resources/data/
```

Estructura esperada:

```text
src/main/resources/data/
├── movimientos_financieros_diarios.csv
├── intereses_trimestrales.csv
└── estados_financieros_anuales.csv
```

---

# 7. Cargar `.env` en PowerShell

Docker Compose lee automáticamente el archivo `.env`.

Para ejecutar directamente el JAR del backend desde PowerShell, se pueden cargar las variables en la sesión actual:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()

        if (
            ($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))
        ) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        Set-Item -Path "Env:$name" -Value $value
    }
}
```

---

# 8. Compilar el backend principal

Desde la raíz del proyecto:

```powershell
.\mvnw.cmd clean package -DskipTests
```

El artefacto generado queda en:

```text
target/demo-0.0.1-SNAPSHOT.jar
```

---

# 9. Ejecutar Spring Batch

Se implementaron tres jobs.

Para evitar conflictos de puerto, se recomienda ejecutar estos procesos antes de levantar el ecosistema Docker completo o detener temporalmente el contenedor `backend`.

## 9.1 Reporte de Transacciones Diarias

```powershell
java -jar .\target\demo-0.0.1-SNAPSHOT.jar `
  --spring.batch.job.name=reporteTransaccionesDiariasJob
```

Resultado esperado:

```text
COMPLETED
```

---

## 9.2 Cálculo de Intereses Mensuales

```powershell
java -jar .\target\demo-0.0.1-SNAPSHOT.jar `
  --spring.batch.job.name=calculoInteresesMensualesJob
```

Resultado esperado:

```text
COMPLETED
```

---

## 9.3 Estados de Cuenta Anuales

```powershell
java -jar .\target\demo-0.0.1-SNAPSHOT.jar `
  --spring.batch.job.name=estadosCuentaAnualesJob
```

Resultado esperado:

```text
COMPLETED
```

---

## 9.4 Características batch

Los tres procesos utilizan:

```text
BankPartitioner
TaskExecutorPartitionHandler
gridSize = 5
```

También implementan:

- procesamiento paralelo;
- retry ante fallos transitorios;
- política de skip;
- listeners;
- manejo de errores;
- reinicio ante fallos críticos.

La reejecución automática se implementa mediante:

```text
CriticalFailureRestartListener
```

y:

```text
JobOperator.restart(...)
```

---

# 10. Validar Docker Compose

Antes de levantar el ecosistema:

```powershell
docker compose config --quiet
```

Si la configuración es válida, el comando debe finalizar sin errores.

---

# 11. Construir y levantar el ecosistema

Desde la raíz del proyecto:

```powershell
docker compose up -d --build `
  --scale cuentas-service=2 `
  --scale pagos-service=2 `
  --scale clientes-service=2
```

Comprobar:

```powershell
docker compose ps
```

Se espera encontrar activos:

```text
kafka
discovery-server
config-server
auth-server
backend

cuentas-service x2
pagos-service x2
clientes-service x2

auditoria-service

bff-web
bff-mobile
bff-atm
```

---

# 12. Verificar Eureka

Eureka se ejecuta en:

```text
http://localhost:8761
```

Abrir:

```text
http://localhost:8761
```

Para la ejecución escalada se espera:

```text
CUENTAS-SERVICE       2 instancias
PAGOS-SERVICE         2 instancias
CLIENTES-SERVICE      2 instancias
```

Además deben encontrarse registrados:

```text
AUDITORIA-SERVICE
BFF-WEB
BFF-MOBILE
BFF-ATM
```

---

# 13. Verificar Config Server

Config Server se ejecuta en:

```text
http://localhost:8888
```

Ejemplo:

```powershell
curl.exe -sS http://localhost:8888/cuentas-service/default
```

También pueden verificarse:

```text
bff-web/default
bff-mobile/default
bff-atm/default
pagos-service/default
clientes-service/default
```

---

# 14. Verificar Authorization Server

Authorization Server:

```text
http://localhost:9000
```

Comprobar JWK Set:

```powershell
curl.exe -sS http://localhost:9000/oauth2/jwks
```

Debe retornarse el conjunto de claves públicas utilizado para validar los JWT.

---

# 15. Obtener tokens OAuth2

El flujo utilizado es:

```text
client_credentials
```

## 15.1 Token Web

```powershell
$oauthWeb = curl.exe -sS `
    --user "bankxyz-web:$env:OAUTH_WEB_CLIENT_SECRET" `
    -X POST `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "grant_type=client_credentials" `
    -d "scope=WEB" `
    http://localhost:9000/oauth2/token |
    ConvertFrom-Json

$tokenWeb = $oauthWeb.access_token
```

## 15.2 Token Mobile

```powershell
$oauthMobile = curl.exe -sS `
    --user "bankxyz-mobile:$env:OAUTH_MOBILE_CLIENT_SECRET" `
    -X POST `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "grant_type=client_credentials" `
    -d "scope=MOBILE" `
    http://localhost:9000/oauth2/token |
    ConvertFrom-Json

$tokenMobile = $oauthMobile.access_token
```

## 15.3 Token ATM

```powershell
$oauthAtm = curl.exe -sS `
    --user "bankxyz-atm:$env:OAUTH_ATM_CLIENT_SECRET" `
    -X POST `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "grant_type=client_credentials" `
    -d "scope=ATM" `
    http://localhost:9000/oauth2/token |
    ConvertFrom-Json

$tokenAtm = $oauthAtm.access_token
```

No es necesario mostrar los tokens en pantalla.

---

# 16. Probar BFF Web

BFF Web:

```text
https://localhost:8081
```

Health check:

```powershell
curl.exe -k -sS https://localhost:8081/actuator/health
```

Consultar una cuenta:

```powershell
curl.exe -k -i `
    -H "Authorization: Bearer $tokenWeb" `
    https://localhost:8081/api/web/cuentas/101
```

Resultado esperado:

```text
HTTP 200
```

con los datos reales de la cuenta.

---

# 17. Probar BFF Mobile

BFF Mobile:

```text
https://localhost:8082
```

Health check:

```powershell
curl.exe -k -sS https://localhost:8082/actuator/health
```

Consultar una cuenta:

```powershell
curl.exe -k -i `
    -H "Authorization: Bearer $tokenMobile" `
    https://localhost:8082/api/mobile/cuentas/101
```

Resultado esperado:

```text
HTTP 200
```

---

# 18. Probar BFF ATM

BFF ATM:

```text
https://localhost:8083
```

Health check:

```powershell
curl.exe -k -sS https://localhost:8083/actuator/health
```

Consultar saldo:

```powershell
curl.exe -k -i `
    -H "Authorization: Bearer $tokenAtm" `
    https://localhost:8083/api/atm/cuentas/101/saldo
```

---

# 19. Probar autorización por canal

Los scopes son:

```text
WEB
MOBILE
ATM
```

Un token de un canal distinto no debe poder acceder a otro BFF.

Ejemplo: utilizar un token `MOBILE` contra Web:

```powershell
curl.exe -k -i `
    -H "Authorization: Bearer $tokenMobile" `
    https://localhost:8081/api/web/cuentas/101
```

Resultado esperado:

```text
403 Forbidden
```

---

# 20. Probar depósito mediante BFF Web

Endpoint:

```text
POST /api/web/depositos
```

Crear el body:

```powershell
$body = '{"cuentaId":101,"monto":1.00,"descripcion":"Prueba Kafka EFT"}'
```

Ejecutar:

```powershell
$body | curl.exe -k -i `
    -X POST `
    -H "Authorization: Bearer $tokenWeb" `
    -H "Content-Type: application/json" `
    --data-binary "@-" `
    "https://localhost:8081/api/web/depositos"
```

Resultado esperado:

```text
HTTP 200
tipo = DEPOSITO
estado = COMPLETADA
```

La respuesta contiene, entre otros:

```text
operacionId
cuentaDestinoId
monto
saldoDestino
estado
mensaje
fechaHora
```

---

# 21. Probar pago mediante BFF Web

Endpoint:

```text
POST /api/web/pagos
```

Body:

```json
{
  "cuentaId": 101,
  "monto": 1.00,
  "descripcion": "Prueba pago EFT"
}
```

PowerShell:

```powershell
$body = '{"cuentaId":101,"monto":1.00,"descripcion":"Prueba pago EFT"}'

$body | curl.exe -k -i `
    -X POST `
    -H "Authorization: Bearer $tokenWeb" `
    -H "Content-Type: application/json" `
    --data-binary "@-" `
    "https://localhost:8081/api/web/pagos"
```

---

# 22. Probar transferencia mediante BFF Web

Endpoint:

```text
POST /api/web/transferencias
```

Contrato:

```json
{
  "cuentaOrigenId": 101,
  "cuentaDestinoId": 102,
  "monto": 1.00,
  "descripcion": "Prueba transferencia EFT"
}
```

Se deben utilizar cuentas válidas existentes en Oracle.

---

# 23. Probar retiro ATM

Endpoint:

```text
POST /api/atm/cuentas/101/retiro
```

Body:

```json
{
  "monto": 1
}
```

PowerShell:

```powershell
$body = '{"monto":1}'

$body | curl.exe -k -i `
    -X POST `
    -H "Authorization: Bearer $tokenAtm" `
    -H "Content-Type: application/json" `
    --data-binary "@-" `
    "https://localhost:8083/api/atm/cuentas/101/retiro"
```

Las operaciones monetarias ATM no utilizan retry automático en el BFF para evitar duplicar una transacción.

---

# 24. Verificar Kafka

Kafka utiliza:

```text
Host:   localhost:9092
Docker: kafka:29092
```

Tópicos implementados:

```text
retiro-realizado
pago-realizado
deposito-realizado
transferencia-realizada
```

Productores:

```text
cuentas-service
pagos-service
```

Consumidor independiente:

```text
auditoria-service
```

---

# 25. Verificar Auditoría

Después de realizar una operación monetaria:

```powershell
docker compose logs --tail=100 auditoria-service
```

Para comprobar específicamente un depósito:

```powershell
docker compose logs --since=5m auditoria-service |
    Select-String "deposito-realizado"
```

Se espera una entrada similar a:

```text
AUDITORIA MICROSERVICIO
topic=deposito-realizado
key=<operacionId>
particion=<n>
offset=<n>
evento=<json>
```

El `operacionId` debe coincidir con el retornado por la operación original.

---

# 26. Verificar los microservicios

Puertos internos Docker:

```text
cuentas-service    8084
pagos-service      8085
clientes-service   8086
auditoria-service  8087
```

Ejemplo de health check desde `bankxyz-network`:

```powershell
docker run --rm `
    --network bankxyz-network `
    curlimages/curl:8.10.1 `
    -sS http://cuentas-service:8084/actuator/health
```

Se espera comprobar:

```text
status = UP
db = UP
Config Server = UP
Eureka = UP
```

Repetir para:

```text
pagos-service:8085
clientes-service:8086
```

---

# 27. Verificar escalabilidad horizontal

Levantar dos instancias de los tres microservicios principales:

```powershell
docker compose up -d `
    --scale cuentas-service=2 `
    --scale pagos-service=2 `
    --scale clientes-service=2 `
    cuentas-service pagos-service clientes-service
```

Comprobar:

```powershell
docker compose ps cuentas-service pagos-service clientes-service
```

Deben aparecer:

```text
cuentas-service     2
pagos-service       2
clientes-service    2
```

Eureka debe registrar además las dos instancias de cada aplicación con estado `UP`.

---

# 28. Verificar Resilience4j

Resilience4j protege principalmente las llamadas:

```text
BFF -> Microservicio
```

BFF Web utiliza en operaciones de lectura:

```text
Circuit Breaker
Retry
Bulkhead
Fallback
```

BFF Mobile y ATM incorporan Circuit Breaker y fallback en sus comunicaciones correspondientes.

Las operaciones monetarias no utilizan retry automático.

Cuando un servicio no responde correctamente, el BFF puede entregar una respuesta controlada, por ejemplo:

```text
TEMPORALMENTE_NO_DISPONIBLE
```

---

# 29. Consultar logs

BFF Web:

```powershell
docker compose logs --tail=100 bff-web
```

Cuentas:

```powershell
docker compose logs --tail=100 cuentas-service
```

Pagos:

```powershell
docker compose logs --tail=100 pagos-service
```

Auditoría:

```powershell
docker compose logs --tail=100 auditoria-service
```

Todos:

```powershell
docker compose logs --tail=100
```

---

# 30. Problemas comunes

## Token expirado

Síntoma:

```text
401 Unauthorized
Jwt expired
```

Solicitar un nuevo token OAuth2.

---

## Certificado autofirmado

Si `curl` rechaza el certificado HTTPS de desarrollo:

```text
-k
```

---

## Wallet no encontrado

Verificar:

```text
ORACLE_WALLET_HOST_PATH
```

y confirmar que el directorio contiene los archivos del Wallet.

---

## Primera conexión Oracle lenta

La primera consulta puede requerir algunos segundos mientras HikariCP inicializa el pool de conexiones.

Las consultas posteriores reutilizan las conexiones disponibles.

---

## Servicio no registrado en Eureka

Comprobar:

```powershell
docker compose ps
```

y:

```powershell
docker compose logs --tail=100 <servicio>
```

---

## Las réplicas vuelven a una instancia

Una ejecución posterior de `docker compose up` puede reconciliar un servicio con la escala por defecto.

Volver a ejecutar:

```powershell
docker compose up -d `
    --scale cuentas-service=2 `
    --scale pagos-service=2 `
    --scale clientes-service=2 `
    cuentas-service pagos-service clientes-service
```

---

# 31. Detener el entorno

Detener los contenedores:

```powershell
docker compose down
```

Para eliminar además los volúmenes Docker:

```powershell
docker compose down -v
```

La opción `-v` debe utilizarse únicamente cuando también se quieran eliminar los volúmenes asociados.

---

# 32. Resultado esperado

Al finalizar las pruebas se debe poder verificar:

```text
SPRING BATCH
reporteTransaccionesDiariasJob      COMPLETED
calculoInteresesMensualesJob        COMPLETED
estadosCuentaAnualesJob             COMPLETED

INFRAESTRUCTURA
Eureka                              UP
Config Server                       UP
Authorization Server                UP
Kafka                               UP

BFF
BFF Web HTTPS                       UP
BFF Mobile HTTPS                    UP
BFF ATM HTTPS                       UP

MICROSERVICIOS
Cuentas Service                     UP
Pagos Service                       UP
Clientes Service                    UP
Auditoria Service                   UP

SEGURIDAD
OAuth2                              OK
JWT                                 OK
Scopes por canal                    OK
Scope incorrecto                    403

BASE DE DATOS
Oracle Autonomous Database          UP

RESILIENCIA
Circuit Breaker                     configurado
Retry de lecturas                   configurado
Bulkhead                            configurado
Fallback                            operativo

KAFKA
Productores                         operativos
Consumidor independiente            operativo
Eventos end-to-end                  verificables

ESCALABILIDAD
Cuentas Service                     2 réplicas
Pagos Service                       2 réplicas
Clientes Service                    2 réplicas
```

---

# 33. Despliegue en la nube

Este documento describe la ejecución y las pruebas funcionales del sistema.

El procedimiento completo para desplegar Bank XYZ en AWS EC2 se encuentra en:

```text
despliegue.md
```