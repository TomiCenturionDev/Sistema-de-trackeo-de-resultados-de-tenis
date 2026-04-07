## 🧩 Descripción del proyecto
Este proyecto simula un sistema de integración backend donde la consistencia de datos no depende de sistemas externos.

La aplicación procesa resultados de partidos de tenis desde una API, aplica reglas de negocio y los sincroniza con Salesforce.

💡 El resultado es un sistema desacoplado y tolerante a fallos, alineado con escenarios reales de integración.

## 💡 ¿Qué problemas resuelve?

> ❗ ¿Qué pasa cuando una integración falla?

Para evitar pérdida de datos, se implementa una persistencia intermedia, almacenando primero la información en una base local y sincronizándola luego de forma controlada.

Esto permite:

- Mantener consistencia ante fallos externos
- Evitar duplicados mediante idempotencia
- Reintentar sincronizaciones sin pérdida de información


## 🔄 Flujos del sistema

### ✅ Caso exitoso
- Se recibe el partido desde la API
- Se valida según reglas de negocio
- Se persiste en base de datos
- Se sincroniza con Salesforce
- Estado final: `SYNCED`

### ⚠️ Error reintentable (ej: falla de integración con Salesforce)
- El partido se persiste correctamente
- La sincronización falla
- El registro queda en estado PENDING
- Se reintenta la sincronización
- No se pierde información

### ❌ Error no reintentable (ej: datos inválidos)
- Se detecta un error de negocio
- No se sincroniza con Salesforce
- Se registra el error
- No se realizan reintentos


## 🔄 Tolerancia a fallos (resiliencia)

La arquitectura desacoplada permite que:

- El backend funcione independientemente de Salesforce.
- Ante fallos o micro-cortes, los datos se preserven en la base local.
- La sincronización pueda retomarse sin pérdida de información.

## 🏗️ Arquitectura del sistema

El sistema sigue un flujo desacoplado donde la persistencia local actúa como punto central:

```
API externa
     ↓
Backend (Spring Boot)
     ↓
PostgreSQL
   ↙     ↘
Salesforce   Servicio SOAP
  (REST)    (enriquecimiento)
```

💡 La persistencia local desacopla el procesamiento de la disponibilidad de servicios externos, permitiendo tolerancia a fallos y reintentos seguros.


## 🛠️ Tecnologías utilizadas:

* **Java + Spring Boot**
  Implementación de la lógica de negocio y orquestación del flujo de integración.
* **PostgreSQL + Hibernate (JPA)**
  Persistencia local utilizada como punto central para garantizar consistencia y tolerancia a fallos.
* **Salesforce API (REST)**
  Sistema externo donde se sincronizan los datos procesados.
* **SOAP (cliente generado desde WSDL)**
  Servicio utilizado para enriquecer los datos antes de su envío a Salesforce
* **OpenFeign**
  Cliente HTTP utilizado para consumir servicios externos de forma declarativa.
* **Postman**
  Pruebas manuales de endpoints y validación de flujos de integración

## 🔌 Integraciones

### 🌐 REST (Salesforce)

Utilizado para la comunicación con Salesforce, permitiendo la sincronización de los datos procesados.

### 🧾 SOAP (Enriquecimiento)

El servicio SOAP recibe un código de país (ISO) y retorna la capital correspondiente, utilizada para enriquecer los datos antes de su sincronización.

### 📥 Ejemplo de request SOAP

```xml
<GetCapitalRequest>
    <CountryCode>AR</CountryCode>
</GetCapitalRequest>
```
> El cliente SOAP es generado a partir del WSDL, encapsulando automáticamente la estructura del request.

### 💡 Valor agregado:

- Enriquecimiento automático de datos antes de su persistencia
- Mejora en la calidad de la información sincronizada

## 🚀 Highlights

- Persistencia intermedia para evitar pérdida de datos
- Manejo de errores con reintentos controlados
- Prevención de duplicados mediante idempotencia
- Integración con sistemas externos (REST + SOAP)
- Consistencia entre sistemas desacoplados

## 🚧 Desafíos técnicos abordados:

- **Prevención de duplicados en integraciones externas**
  → Resuelto mediante uso de External IDs para garantizar idempotencia

- **Tolerancia a fallos en servicios externos (Salesforce)**
  → Resuelto mediante persistencia local para evitar pérdida de datos

- **Integración de SOAP en una arquitectura moderna REST**
  → Integrado mediante cliente generado desde WSDL

- **Consistencia entre sistemas desacoplados**
  → Implementado mediante sincronización desacoplada entre persistencia local y sistemas externos


## ⚙️ Guía de Instalación 

## Requisitos Previos:

- Java 17 instalado.

- PostgreSQL en ejecución

- Maven Wrapper incluido


## 🗄️ Base de datos
Crear una base de datos PostgreSQL:
```tenis_stats_db```

Configurar en:
```src/main/resources/application.properties```

```
- spring.datasource.url=jdbc:postgresql://localhost:5432/tenis_stats_db
- spring.datasource.username=TU_USUARIO
- spring.datasource.password=TU_PASSWORD

- spring.jpa.hibernate.ddl-auto=update
- spring.jpa.show-sql=true
```

## ▶️ Ejecución

### Windows
```
- mvnw clean install
- mvnw spring-boot:run
```

### Linux / Mac

```
./mvnw clean install
./mvnw spring-boot:run
```


# ☁️ Configuración en Salesforce

``` Crear un objeto llamado: Partido__c. ```

Configurar:
```
- salesforce.client.id=tu_id_del_cliente.
- salesforce.username=tu_usuario.
- salesforce.login.url=https://login.salesforce.com.
- salesforce.instance.url=https://tu-instancia.my.salesforce.com.
- salesforce.partido.endpoint=/services/data/v60.0/sobjects/Partido__c.
```

## 📡 Endpoint principal
```
POST /api/partidos/registrar  
Base URL: http://localhost:8080
```
Permite registrar un partido, procesar su resultado y sincronizarlo con Salesforce.

## 🧪 Casos de prueba

Estos ejemplos permiten probar distintos escenarios del sistema:

```
Caso A: Grand Slam

{
"id": 5,
"fecha": "2024-06-09",
"torneo": "Roland Garros",
"pais": "FR",
"ciudad": "Paris",
"jugador1": {
"id": 2,
"nombre": "Carlos Alcaraz",
"nacionalidad": "ES",
"rankingActual": 2
},
"jugador2": {
"id": 5,
"nombre": "Novak Djokovic",
"nacionalidad": "RS",
"rankingActual": 3
},
"categoria": {
"id": 1,
"nombre": "Grand Slam",
"puntos": 2000,
"tipoDeTorneo": "GS"
},
"superficie": "ARCILLA",
"fase": "F",
"estado": "FINALIZADO",
"resultado": "6-3 2-6 7-5 1-6 6-2",
"ganador": "Carlos Alcaraz"
}
```

```
Caso B: Retiro para ATP

{
  "fecha": "2014-05-31",
  "torneo": "Masters 1000 Roma",
  "pais": "IT",
  "ciudad": "Roma",
  "jugador1": {
    "id": 2,
    "nombre": "Carlos Alcaraz",
    "nacionalidad": "ES",
    "rankingActual": 2
  },
  "jugador2": {
    "id": 5,
    "nombre": "Novak Djokovic",
    "nacionalidad": "RS",
    "rankingActual": 3
  },
  "categoria": {
    "id": 4,
    "nombre": "Masters 1000",
    "puntos": 1000,
    "tipoDeTorneo": "M1000"
  },
  "superficie": "ARCILLA",
  "fase": "R32",
  "estado": "FINALIZADO",
  "resultado": "6-3 1-0 RET",
  "ganador": "Carlos Alcaraz",
  "jugadorRetirado": ""
}}
```

```
Caso C para partido común de ATP, pero con un Tie-break

{
{
  "fecha": "2014-05-31",
  "torneo": "Masters 1000 Roma",
  "pais": "IT",
  "ciudad": "Roma",
  "jugador1": {
    "id": 2,
    "nombre": "Carlos Alcaraz",
    "nacionalidad": "ES",
    "rankingActual": 2
  },
  "jugador2": {
    "id": 5,
    "nombre": "Novak Djokovic",
    "nacionalidad": "RS",
    "rankingActual": 3
  },
  "categoria": {
    "id": 4,
    "nombre": "Masters 1000",
    "puntos": 1000,
    "tipoDeTorneo": "M1000"
  },
  "superficie": "ARCILLA",
  "fase": "F",
  "estado": "FINALIZADO",
  "resultado": "6-3 6-4",
  "ganador": "Carlos Alcaraz",
  "jugadorRetirado": ""
}
```

## 🧪 Estrategia de QA

Se definieron casos de prueba orientados a validar las reglas de negocio y el comportamiento del sistema ante distintos escenarios:

- **Partido al mejor de 5 sets (Grand Slam)**  
  → Validación de reglas específicas según tipo de torneo

- **Partido con retiro ("RET")**  
  → Manejo de resultados incompletos y determinación de ganador

- **Partido estándar (ATP o Challenger, al mejor de 3 sets)**  
  → Validación de flujo normal y procesamiento de resultados

## 🎯 Valor del proyecto

Este proyecto demuestra la capacidad de diseñar integraciones donde la consistencia de datos no depende de sistemas externos.

A través de persistencia intermedia, manejo de errores y sincronización controlada, el sistema permite:

- Evitar pérdida de datos ante fallos de integración
- Mantener consistencia entre sistemas desacoplados
- Prevenir duplicados mediante idempotencia


## 🔄 Enfoque de desarrollo

El proyecto fue desarrollado de forma iterativa, priorizando la implementación y validación progresiva de las funcionalidades clave:

- Implementación incremental de reglas de negocio
- Validación mediante pruebas manuales de los flujos de integración
- Ajustes a partir de errores detectados durante el procesamiento
- Enfoque en integraciones críticas (persistencia y sincronización con Salesforce)

## 💻 Sobre el proyecto
Proyecto desarrollado de forma individual enfocado en resolver problemas reales de integración, priorizando consistencia de datos, manejo de errores y desacople entre sistemas.

## 🧠 Decisiones de diseño

- Uso de persistencia intermedia para desacoplar el sistema de integraciones externas
- Uso del patrón de diseño strategy para dividir distintos tipos de torneos
- Manejo de estados (PENDING, SYNCED, FAILED) para controlar el flujo de sincronización
- Idempotencia mediante identificadores únicos para evitar duplicados en reintentos
- Persistencia de estados de sincronización para permitir reintentos controlados

## 🔮 Posibles mejoras

- Mejorar el modelado de fases y rondas para evitar redundancia de datos
- Ampliar cobertura de tests para validar reglas de negocio, idempotencia y manejo de errores
- Mejorar el modelado del resultado de partidos en una estructura de sets
- Implementar sincronización diferida mediante jobs programados 