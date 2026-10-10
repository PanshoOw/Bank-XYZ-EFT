# Despliegue en AWS — Bank XYZ EFT

Este documento describe el procedimiento utilizado para desplegar la arquitectura de **Bank XYZ EFT** en Amazon Web Services mediante una instancia Amazon EC2, Docker y Docker Compose.

El despliegue permite ejecutar los tres microservicios principales con escalabilidad horizontal:

```text
cuentas-service   x2
pagos-service     x2
clientes-service  x2
```

junto con:

```text
Eureka Server
Config Server
Authorization Server
Apache Kafka
Auditoria Service
BFF Web
BFF Mobile
BFF ATM
```

La solución fue desplegada y validada funcionalmente en AWS.

---

# 1. Arquitectura de despliegue

Para la evaluación se utilizó una instancia Amazon EC2 como host de Docker.

Dentro de EC2 se ejecutan contenedores independientes conectados mediante una red Docker.

```text
Internet
   |
   v
AWS Security Group
   |
   v
Amazon EC2
   |
   +-- BFF Web HTTPS
   +-- BFF Mobile HTTPS
   +-- BFF ATM HTTPS
   |
   +-- Authorization Server
   +-- Eureka Server
   +-- Config Server
   +-- Apache Kafka
   |
   +-- Cuentas Service x2
   +-- Pagos Service x2
   +-- Clientes Service x2
   |
   +-- Auditoria Service
   |
   v
Oracle Autonomous Database
```

Los servicios internos se comunican mediante:

```text
bankxyz-network
```

Los microservicios de negocio no se exponen directamente a Internet.

---

# 2. Infraestructura utilizada

Configuración utilizada durante la validación:

| Recurso | Configuración |
|---|---|
| Servicio | Amazon EC2 |
| Sistema operativo | Ubuntu Server 24.04 LTS |
| Arquitectura | x86_64 |
| Tipo de instancia | `t3.large` |
| CPU | 2 vCPU |
| Memoria | aprox. 8 GiB |
| Disco | 30 GiB gp3 |
| Contenedores | Docker |
| Orquestación | Docker Compose v2 |

Se utilizó una única instancia EC2 para reducir la complejidad y el consumo de créditos del entorno académico.

La escalabilidad horizontal se implementa dentro del host mediante múltiples contenedores independientes.

---

# 3. Crear la instancia EC2

Desde AWS:

```text
EC2
→ Instances
→ Launch instances
```

Configuración recomendada:

```text
Name: bankxyz-eft

AMI:
Ubuntu Server 24.04 LTS

Architecture:
64-bit (x86)

Instance type:
t3.large

Storage:
30 GiB gp3
```

Habilitar dirección IPv4 pública para poder acceder a los BFF durante la demostración.

---

# 4. Crear el Key Pair

Durante la creación de la instancia, generar un nuevo par de claves.

Ejemplo:

```text
Name:
bankxyz-eft-key

Type:
RSA

Format:
.pem
```

Guardar el archivo descargado, por ejemplo:

```text
C:\Users\<usuario>\Downloads\bankxyz-eft-key.pem
```

El archivo privado no debe incluirse en Git.

---

# 5. Configurar el Security Group

El Security Group utilizado permite únicamente los puertos necesarios para administración y demostración.

Reglas de entrada:

| Puerto | Protocolo | Función |
|---:|---|---|
| 22 | TCP | SSH |
| 8081 | TCP | BFF Web HTTPS |
| 8082 | TCP | BFF Mobile HTTPS |
| 8083 | TCP | BFF ATM HTTPS |
| 8761 | TCP | Eureka Dashboard |
| 9000 | TCP | OAuth2 Authorization Server |

Para una evaluación académica se recomienda utilizar:

```text
Source = My IP
```

en lugar de:

```text
0.0.0.0/0
```

Esto limita el acceso a la IP pública del equipo del evaluador o desarrollador.

No es necesario exponer públicamente:

```text
8080
8084
8085
8086
8087
8888
9092
29092
```

Los microservicios, Config Server y Kafka se comunican internamente mediante Docker.

---

# 6. Obtener la IP pública

Después de iniciar EC2, consultar:

```text
EC2
→ Instances
→ Public IPv4 address
```

En los siguientes ejemplos se utiliza:

```text
<IP_PUBLICA_EC2>
```

como marcador.

Ejemplo:

```text
ubuntu@<IP_PUBLICA_EC2>
```

---

# 7. Conectarse mediante SSH

Esto se ejecuta en **PowerShell de Windows**:

```powershell
ssh -i "$HOME\Downloads\bankxyz-eft-key.pem" ubuntu@<IP_PUBLICA_EC2>
```

Una conexión correcta muestra:

```text
ubuntu@ip-172-xx-xx-xx:~$
```

---

# 8. Instalar Docker en EC2

Los siguientes comandos se ejecutan en **Ubuntu/EC2**.

Actualizar repositorios:

```bash
sudo apt update
```

Instalar herramientas:

```bash
sudo apt install -y \
  docker.io \
  docker-compose-v2 \
  git \
  unzip
```

Activar Docker:

```bash
sudo systemctl enable --now docker
```

Agregar el usuario `ubuntu` al grupo Docker:

```bash
sudo usermod -aG docker ubuntu
```

Aplicar el grupo en la sesión actual:

```bash
newgrp docker
```

Verificar:

```bash
docker --version
docker compose version
git --version
```

---

# 9. Crear swap

La instancia utilizada dispone de aproximadamente 8 GiB de RAM.

Debido a que el ecosistema ejecuta múltiples JVM simultáneamente, se configuraron 2 GiB adicionales de swap.

Ejecutar una sola vez:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

Hacerlo persistente:

```bash
if ! grep -q '^/swapfile ' /etc/fstab; then
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi
```

Verificar:

```bash
free -h
swapon --show
```

Resultado esperado aproximado:

```text
RAM:   7.6 GiB
Swap:  2.0 GiB
```

---

# 10. Preparar una copia limpia del proyecto

Para evitar copiar:

```text
target/
.git/
credenciales
archivos temporales
```

se genera un ZIP únicamente con archivos versionados en Git.

Esto se ejecuta en **PowerShell de Windows**, desde el proyecto:

```powershell
git status --short
```

El resultado debe estar vacío.

Crear archivo:

```powershell
git archive `
    --format=zip `
    --output="$HOME\Downloads\Proyecto_EFT_AWS.zip" `
    HEAD
```

---

# 11. Transferir el proyecto a EC2

Desde **PowerShell de Windows**:

```powershell
scp `
    -i "$HOME\Downloads\bankxyz-eft-key.pem" `
    "$HOME\Downloads\Proyecto_EFT_AWS.zip" `
    ubuntu@<IP_PUBLICA_EC2>:/home/ubuntu/
```

---

# 12. Descomprimir el proyecto

En **Ubuntu/EC2**:

```bash
mkdir -p ~/Proyecto_EFT
```

Descomprimir:

```bash
unzip ~/Proyecto_EFT_AWS.zip -d ~/Proyecto_EFT
```

Entrar al proyecto:

```bash
cd ~/Proyecto_EFT
```

---

# 13. Mantener secretos fuera del repositorio

Crear un directorio separado:

```bash
mkdir -p ~/bankxyz-secrets
chmod 700 ~/bankxyz-secrets
```

En este directorio se almacena el Oracle Wallet.

Ejemplo:

```text
/home/ubuntu/bankxyz-secrets/Wallet_BANKXYZ/
```

El directorio real debe contener:

```text
cwallet.sso
tnsnames.ora
sqlnet.ora
```

entre los demás archivos del Wallet.

---

# 14. Transferir Oracle Wallet

Desde **PowerShell de Windows**:

```powershell
scp `
    -i "$HOME\Downloads\bankxyz-eft-key.pem" `
    -r `
    "C:\ruta\al\Wallet_BANKXYZ" `
    ubuntu@<IP_PUBLICA_EC2>:/home/ubuntu/bankxyz-secrets/
```

En EC2 comprobar:

```bash
find ~/bankxyz-secrets -name cwallet.sso
```

La carpeta que contiene directamente:

```text
cwallet.sso
tnsnames.ora
sqlnet.ora
```

debe utilizarse como:

```text
ORACLE_WALLET_HOST_PATH
```

---

# 15. Transferir `.env`

El archivo `.env` no forma parte del repositorio.

Transferir desde Windows:

```powershell
scp `
    -i "$HOME\Downloads\bankxyz-eft-key.pem" `
    ".\.env" `
    ubuntu@<IP_PUBLICA_EC2>:/home/ubuntu/Proyecto_EFT/.env
```

En EC2:

```bash
chmod 600 ~/Proyecto_EFT/.env
```

Editar:

```bash
nano ~/Proyecto_EFT/.env
```

Configurar:

```text
ORACLE_WALLET_HOST_PATH=/home/ubuntu/bankxyz-secrets/<DIRECTORIO_WALLET>
```

No mostrar en capturas ni videos:

```text
contraseñas
client secrets
tokens
credenciales
```

---

# 16. Transferir los keystores

Los tres BFF requieren:

```text
keystore.p12
```

Transferir BFF Web:

```powershell
scp `
    -i "$HOME\Downloads\bankxyz-eft-key.pem" `
    ".\bff-web\src\main\resources\keystore.p12" `
    ubuntu@<IP_PUBLICA_EC2>:/home/ubuntu/Proyecto_EFT/bff-web/src/main/resources/keystore.p12
```

BFF Mobile:

```powershell
scp `
    -i "$HOME\Downloads\bankxyz-eft-key.pem" `
    ".\bff-mobile\src\main\resources\keystore.p12" `
    ubuntu@<IP_PUBLICA_EC2>:/home/ubuntu/Proyecto_EFT/bff-mobile/src/main/resources/keystore.p12
```

BFF ATM:

```powershell
scp `
    -i "$HOME\Downloads\bankxyz-eft-key.pem" `
    ".\bff-atm\src\main\resources\keystore.p12" `
    ubuntu@<IP_PUBLICA_EC2>:/home/ubuntu/Proyecto_EFT/bff-atm/src/main/resources/keystore.p12
```

---

# 17. Validación previa al despliegue

Entrar al proyecto:

```bash
cd ~/Proyecto_EFT
```

Validar Docker Compose:

```bash
docker compose config --quiet
```

Resultado esperado:

```text
sin errores
```

También se debe verificar que:

```text
.env existe
Oracle Wallet existe
los tres keystore.p12 existen
```

---

# 18. Construir las imágenes

Para evitar un consumo excesivo de memoria durante el build se puede limitar el paralelismo:

```bash
export COMPOSE_PARALLEL_LIMIT=1
```

Construir:

```bash
docker compose build
```

El proyecto construye imágenes para los distintos componentes Spring Boot.

Comprobar:

```bash
docker images | grep bankxyz
```

---

# 19. Levantar infraestructura base

Primero se levantan los servicios de infraestructura:

```bash
docker compose up -d \
  kafka \
  discovery-server \
  config-server \
  auth-server
```

Esperar:

```bash
sleep 30
```

Comprobar:

```bash
docker compose ps \
  kafka \
  discovery-server \
  config-server \
  auth-server
```

---

# 20. Verificar infraestructura

## Eureka

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  http://localhost:8761/
```

Esperado:

```text
HTTP 200
```

## Config Server

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  http://localhost:8888/cuentas-service/default
```

Esperado:

```text
HTTP 200
```

## Authorization Server

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  http://localhost:9000/oauth2/jwks
```

Esperado:

```text
HTTP 200
```

---

# 21. Levantar los microservicios con escalabilidad horizontal

Ejecutar:

```bash
docker compose up -d \
  --scale cuentas-service=2 \
  --scale pagos-service=2 \
  --scale clientes-service=2 \
  cuentas-service \
  pagos-service \
  clientes-service
```

Esperar:

```bash
sleep 45
```

Comprobar:

```bash
docker compose ps \
  cuentas-service \
  pagos-service \
  clientes-service
```

Se deben visualizar:

```text
cuentas-service    x2
pagos-service      x2
clientes-service   x2
```

---

# 22. Validar Eureka

Ejecutar en EC2:

```bash
python3 - <<'PY'
import urllib.request
import xml.etree.ElementTree as ET

data = urllib.request.urlopen(
    "http://localhost:8761/eureka/apps"
).read()

root = ET.fromstring(data)

print(f"{'Servicio':22} {'Total':>5} {'UP':>5}")
print("-" * 35)

for app in sorted(
    root.findall("application"),
    key=lambda x: x.findtext("name", "")
):
    name = app.findtext("name", "")
    instances = app.findall("instance")

    up = sum(
        1 for i in instances
        if i.findtext("status") == "UP"
    )

    print(f"{name:22} {len(instances):5} {up:5}")
PY
```

Resultado esperado para los microservicios principales:

```text
CLIENTES-SERVICE           2     2
CUENTAS-SERVICE            2     2
PAGOS-SERVICE              2     2
```

---

# 23. Verificar Oracle desde los microservicios

Los endpoints `/actuator/health` permiten comprobar:

```text
Oracle
Config Server
Eureka
```

Para verificar Cuentas:

```bash
docker run --rm \
  --network bankxyz-network \
  curlimages/curl:8.10.1 \
  -sS http://cuentas-service:8084/actuator/health
```

Pagos:

```bash
docker run --rm \
  --network bankxyz-network \
  curlimages/curl:8.10.1 \
  -sS http://pagos-service:8085/actuator/health
```

Clientes:

```bash
docker run --rm \
  --network bankxyz-network \
  curlimages/curl:8.10.1 \
  -sS http://clientes-service:8086/actuator/health
```

Debe observarse:

```text
status = UP
db = UP
database = Oracle
Config Server = UP
Eureka = UP
```

---

# 24. Verificar réplicas individuales

Para comprobar una instancia específica de Cuentas:

```bash
docker compose ps -q cuentas-service
```

Obtener sus IP internas:

```bash
for c in $(docker compose ps -q cuentas-service); do
    docker inspect -f \
      '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' \
      "$c"
done
```

Luego puede consultarse cada IP:

```bash
docker run --rm \
  --network bankxyz-network \
  curlimages/curl:8.10.1 \
  -sS http://<IP_INTERNA>:8084/actuator/health
```

Este método evita utilizar el nombre generado del contenedor como cabecera HTTP `Host`.

---

# 25. Levantar Auditoría

```bash
docker compose up -d auditoria-service
```

Esperar:

```bash
sleep 20
```

Comprobar:

```bash
docker compose ps auditoria-service
```

Logs:

```bash
docker compose logs --tail=100 auditoria-service
```

El consumidor debe unirse correctamente al grupo:

```text
auditoria-service
```

y recibir asignación de las particiones de Kafka.

---

# 26. Levantar los BFF

Ejecutar:

```bash
docker compose up -d \
  bff-web \
  bff-mobile \
  bff-atm
```

Esperar:

```bash
sleep 30
```

Comprobar:

```bash
docker compose ps \
  bff-web \
  bff-mobile \
  bff-atm
```

---

# 27. Restablecer la escala final

Al ejecutar nuevamente `docker compose up`, Compose puede reconciliar las dependencias con una única réplica.

Por este motivo, después de levantar los BFF se vuelve a establecer la escala:

```bash
docker compose up -d \
  --scale cuentas-service=2 \
  --scale pagos-service=2 \
  --scale clientes-service=2 \
  cuentas-service \
  pagos-service \
  clientes-service
```

Verificar nuevamente Eureka.

El estado esperado final es:

```text
AUDITORIA-SERVICE          1     1
BFF-ATM                    1     1
BFF-MOBILE                 1     1
BFF-WEB                    1     1
CLIENTES-SERVICE           2     2
CUENTAS-SERVICE            2     2
PAGOS-SERVICE              2     2
```

---

# 28. Verificar los BFF dentro de EC2

BFF Web:

```bash
curl -k -sS \
  https://localhost:8081/actuator/health
```

BFF Mobile:

```bash
curl -k -sS \
  https://localhost:8082/actuator/health
```

BFF ATM:

```bash
curl -k -sS \
  https://localhost:8083/actuator/health
```

Se espera:

```text
status = UP
```

Los health checks también permiten verificar que los BFF obtienen correctamente el registro desde Eureka.

---

# 29. Verificar acceso desde Internet

Desde **PowerShell de Windows**:

BFF Web:

```powershell
curl.exe -k -sS `
  https://<IP_PUBLICA_EC2>:8081/actuator/health
```

BFF Mobile:

```powershell
curl.exe -k -sS `
  https://<IP_PUBLICA_EC2>:8082/actuator/health
```

BFF ATM:

```powershell
curl.exe -k -sS `
  https://<IP_PUBLICA_EC2>:8083/actuator/health
```

Eureka:

```powershell
curl.exe -sS `
  -o NUL `
  -w "HTTP %{http_code}`n" `
  http://<IP_PUBLICA_EC2>:8761/
```

Resultado esperado:

```text
HTTP 200
```

---

# 30. Obtener token OAuth2 desde AWS

Esto se ejecuta desde **PowerShell de Windows**.

Si las variables `.env` aún no se encuentran cargadas en PowerShell:

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

Solicitar token Web:

```powershell
$oauth = curl.exe -sS `
    --user "bankxyz-web:$env:OAUTH_WEB_CLIENT_SECRET" `
    -X POST `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "grant_type=client_credentials" `
    -d "scope=WEB" `
    http://<IP_PUBLICA_EC2>:9000/oauth2/token |
    ConvertFrom-Json

$tokenWeb = $oauth.access_token
```

Comprobar sin imprimir el token:

```powershell
if ([string]::IsNullOrWhiteSpace($tokenWeb)) {
    throw "No fue posible obtener el token."
}

Write-Host "Token WEB AWS: OK"
```

---

# 31. Prueba end-to-end Cuentas

Consultar una cuenta desde un equipo externo:

```powershell
curl.exe -k -i `
    -H "Authorization: Bearer $tokenWeb" `
    https://<IP_PUBLICA_EC2>:8081/api/web/cuentas/101
```

Resultado esperado:

```text
HTTP/1.1 200
```

junto con los datos reales almacenados en Oracle.

El flujo validado es:

```text
Windows
   |
   v
Authorization Server AWS
   |
   | JWT WEB
   v
BFF Web HTTPS
   |
   v
Eureka
   |
   v
Spring Cloud LoadBalancer
   |
   v
Cuentas Service
   |
   v
Oracle Autonomous Database
```

---

# 32. Consideración sobre cold start de Oracle

Durante la primera llamada a una réplica, HikariCP puede requerir algunos segundos para inicializar el pool de conexiones Oracle.

Si el TimeLimiter de Resilience4j se cumple antes de terminar dicha inicialización, el BFF puede responder temporalmente mediante fallback.

Ejemplo:

```text
TEMPORALMENTE_NO_DISPONIBLE
```

Esto no significa necesariamente que Oracle se encuentre caído.

Comprobar en logs:

```bash
docker compose logs \
  --tail=100 \
  cuentas-service
```

Después de observar:

```text
HikariPool-1 - Added connection
HikariPool-1 - Start completed
```

repetir la consulta.

---

# 33. Prueba Kafka end-to-end

Desde **PowerShell de Windows** crear un depósito de prueba:

```powershell
$body = '{"cuentaId":101,"monto":1.00,"descripcion":"Prueba Kafka AWS EFT"}'
```

Ejecutar:

```powershell
$body | curl.exe -k -i `
    -X POST `
    -H "Authorization: Bearer $tokenWeb" `
    -H "Content-Type: application/json" `
    --data-binary "@-" `
    "https://<IP_PUBLICA_EC2>:8081/api/web/depositos"
```

Se espera:

```text
HTTP 200
tipo = DEPOSITO
estado = COMPLETADA
```

La respuesta contiene:

```text
operacionId
saldoDestino
```

---

# 34. Verificar Kafka y Auditoría

En **Ubuntu/EC2**:

```bash
cd ~/Proyecto_EFT
```

Consultar Auditoría:

```bash
docker compose logs \
  --since=5m \
  auditoria-service 2>&1 |
grep -Ei \
  'deposito-realizado|DEPOSITO'
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

El mismo:

```text
operacionId
```

debe aparecer:

```text
en la respuesta del BFF
y en Auditoria Service
```

Con esto se valida:

```text
Cliente
  |
  v
BFF Web
  |
  v
Pagos Service
 /           \
v             v
Oracle       Kafka
               |
               v
       Auditoria Service
```

---

# 35. Verificar recursos del servidor

Memoria:

```bash
free -h
```

Disco:

```bash
df -h
```

Uso de los contenedores:

```bash
docker stats --no-stream
```

Durante la validación con una instancia `t3.large`, los servicios pudieron ejecutarse con las seis réplicas de los tres microservicios principales y el resto de la infraestructura requerida.

---

# 36. Backend / Spring Batch en AWS

El criterio de despliegue cloud se validó específicamente con:

```text
Cuentas Service
Pagos Service
Clientes Service
```

y los componentes necesarios para su operación distribuida.

Los procesos Spring Batch fueron validados de forma independiente.

Si se desea iniciar también el backend principal en EC2:

```bash
docker compose up -d backend
```

Antes de hacerlo se recomienda verificar:

```bash
free -h
```

debido al número total de JVM ejecutándose en la instancia.

---

# 37. Logs de diagnóstico

BFF Web:

```bash
docker compose logs \
  --tail=100 \
  bff-web
```

Cuentas:

```bash
docker compose logs \
  --tail=100 \
  cuentas-service
```

Pagos:

```bash
docker compose logs \
  --tail=100 \
  pagos-service
```

Clientes:

```bash
docker compose logs \
  --tail=100 \
  clientes-service
```

Auditoría:

```bash
docker compose logs \
  --tail=100 \
  auditoria-service
```

---

# 38. Reiniciar el ecosistema después de detener EC2

Al detener una instancia EC2:

```text
los datos del volumen EBS permanecen
las imágenes Docker permanecen
los archivos del proyecto permanecen
```

Al iniciar nuevamente la instancia, conectarse por SSH y ejecutar:

```bash
cd ~/Proyecto_EFT
```

Luego:

```bash
docker compose up -d \
  --scale cuentas-service=2 \
  --scale pagos-service=2 \
  --scale clientes-service=2
```

Comprobar:

```bash
docker compose ps
```

y posteriormente Eureka.

---

# 39. Cambio de dirección IPv4 pública

Si no se utiliza Elastic IP, al detener y volver a iniciar EC2 la IPv4 pública puede cambiar.

Por este motivo, antes de conectarse nuevamente se debe revisar:

```text
EC2
→ Instances
→ Public IPv4 address
```

y reemplazar:

```text
<IP_PUBLICA_EC2>
```

en los comandos SSH y pruebas externas.

---

# 40. Detener la instancia y costos

Cuando no se esté utilizando el entorno:

```text
EC2
→ Instances
→ Instance state
→ Stop instance
```

Detener la instancia evita continuar consumiendo cómputo EC2.

El volumen EBS se mantiene para conservar:

```text
proyecto
Docker
imágenes
configuración
Wallet
```

y puede continuar generando un costo de almacenamiento.

No seleccionar:

```text
Terminate instance
```

mientras se desee conservar el entorno.

---

# 41. Consideraciones de seguridad

El despliegue utilizado está orientado a una evaluación académica.

Buenas prácticas aplicadas:

- puertos internos no expuestos públicamente;
- Security Group restringido a una IP conocida;
- `.env` fuera de Git;
- Oracle Wallet fuera de Git;
- keystores fuera de Git;
- tokens no almacenados;
- secretos OAuth2 suministrados mediante variables de entorno;
- comunicación HTTPS en los BFF.

Para un entorno productivo se recomienda adicionalmente:

- AWS Secrets Manager;
- Application Load Balancer;
- certificados TLS emitidos por una CA;
- dominio DNS;
- redes privadas;
- NAT Gateway cuando corresponda;
- ECS o EKS;
- autoscaling;
- observabilidad centralizada.

---

# 42. Resultado final validado

El despliegue real permitió comprobar:

```text
AWS EC2                              OK
Ubuntu 24.04                         OK
Docker                               OK
Docker Compose                       OK

Eureka                               UP
Config Server                        UP
Authorization Server                 UP
Kafka                                UP

BFF Web HTTPS                        UP
BFF Mobile HTTPS                     UP
BFF ATM HTTPS                        UP

Cuentas Service                      2/2 UP
Pagos Service                        2/2 UP
Clientes Service                     2/2 UP
Auditoria Service                    UP

Oracle desde Cuentas                 UP
Oracle desde Pagos                   UP
Oracle desde Clientes                UP

OAuth2                               OK
JWT                                  OK
Scope WEB                            OK

Service Discovery                    OK
Load Balancing                       OK

Kafka Producer                       OK
Kafka Consumer independiente         OK

Operación end-to-end                 OK
Escalabilidad horizontal             OK
```

---

# 43. Conclusión

La solución Bank XYZ fue desplegada exitosamente en AWS utilizando Amazon EC2, Docker y Docker Compose.

El despliegue demuestra la capacidad de ejecutar los tres microservicios principales con múltiples instancias:

```text
Cuentas x2
Pagos x2
Clientes x2
```

registradas dinámicamente en Eureka.

También se validó el recorrido completo desde un cliente externo hasta Oracle Autonomous Database y el procesamiento asíncrono de eventos mediante Apache Kafka y Auditoria Service.

La solución cumple así con los objetivos de:

```text
despliegue en la nube
contenerización
escalabilidad horizontal
descubrimiento dinámico
seguridad
resiliencia
mensajería asíncrona
```