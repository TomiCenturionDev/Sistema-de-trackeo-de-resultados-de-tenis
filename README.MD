## 🧩 Descripción del proyecto

Este proyecto consiste en un sistema de integración backend diseñado para sincronizar datos entre una API externa y Salesforce, simulando un escenario real de arquitectura empresarial.

El sistema consume resultados de partidos profesionales de tenis, los procesa aplicando reglas de negocio y los sincroniza con Salesforce, garantizando consistencia y calidad de la información.

Para asegurar resiliencia ante fallos externos, se implementa una estrategia de persistencia intermedia: los datos se almacenan primero en una base local y luego se sincronizan con la nube, desacoplando el flujo principal de la disponibilidad de sistemas externos.

💡 Este enfoque permite construir integraciones robustas, tolerantes a fallos y alineadas con prácticas reales de entornos productivos.

## 💡 ¿Qué problemas resuelve?

## ✅ Eliminación de datos duplicados

Se implementa el uso de External IDs como identificadores únicos. Esto permite:
- Eliminación del 100% de duplicados en reintentos.
- Recuperación automática ante fallos sin pérdida de datos.
- Garantizar unicidad de resultados (Por ejemplo: finales de torneo por año).
- Mantener integridad de datos entre sistemas.

## 🧹 Consistencia y limpieza de datos

El sistema define reglas claras de validación y procesamiento, lo que permite:

- Mantener un historial ordenado.
- Facilitar consultas y auditorías futuras.
- Reducir errores manuales.

## 🔄 Tolerancia a fallos (resiliencia)

La arquitectura desacoplada permite que:

- El backend funcione independientemente de Salesforce.
- Ante fallos o micro-cortes, los datos se preserven en la base local.
- La sincronización pueda retomarse sin pérdida de información.

## 🏗️ Arquitectura del sistema

- API externa de tenis → Backend (Spring Boot)
- Backend → Persistencia en PostgreSQL
- Backend → Integración con Salesforce (REST)
- Backend → Enriquecimiento con servicio SOAP

💡 **La arquitectura desacopla la persistencia local de las integraciones externas, permitiendo tolerancia a fallos y reintentos seguros.**

- Entrada: API externa de resultados.
- Procesamiento: Backend en Spring Boot.
- Persistencia: Base de datos local (PostgreSQL).
- Salida: Salesforce (REST API).
- Enriquecimiento: Servicio externo vía SOAP.


## 🛠️ Tecnologías utilizadas:

- Java + Spring Boot
  Motor principal del sistema, encargado de la lógica de negocio y procesamiento de datos.
- PostgreSQL + Hibernate (JPA)
  Persistencia local robusta y transaccional.
- Salesforce API (REST)
  Destino final donde los datos se convierten en registros utilizables a nivel empresarial.
- SOAP (cliente generado desde WSDL)
  Integración para enriquecimiento de datos externos.
- OpenFeign
  Cliente HTTP declarativo para integración con APIs externas.
- Maven
  Gestión de dependencias y build del proyecto.
- Thunder Client / Postman
  Testeo de endpoints.


## 🔌 Integraciones

## 🌐 REST (JSON)

Utilizado para la comunicación con Salesforce:

- Ligero y eficiente.
- Nativamente soportado por el CRM.
- Ideal para operaciones CRUD de registros.

## 🧾 SOAP (XML)

El servicio SOAP recibe un código de país (ISO) y retorna la capital correspondiente.

Esta información se utiliza para enriquecer los datos antes de persistirlos en Salesforce.
### 📥 Ejemplo de request SOAP

```xml
<GetCapitalRequest>
    <CountryCode>AR</CountryCode>
</GetCapitalRequest>
```
> El request es encapsulado automáticamente dentro de un SOAP Envelope por el cliente generado a partir del WSDL.

### 💡 Valor agregado:

- Enriquecimiento automático de datos
- Reducción de carga manual
- Mejora en la calidad de la información almacenada en Salesforce

## 🚀 Highlights

- Arquitectura resiliente con persistencia intermedia
- Integración híbrida REST + SOAP
- Prevención de duplicados con External IDs
- Sincronización consistente entre sistemas desacoplados
- Diseño preparado para fallos de servicios externos

## 🚧 Desafíos técnicos abordados:

- **Prevención de duplicados en integraciones externas**
  → Resuelto mediante uso de External IDs

- **Tolerancia a fallos en servicios externos (Salesforce)**
  → Implementado mediante persistencia local intermedia

- **Integración de SOAP en una arquitectura moderna REST**
  → Adaptación mediante generación de clientes y procesamiento XML

- **Consistencia entre sistemas desacoplados**
  → Estrategia de sincronización controlada


## ⚙️ Guía de Instalación y Testeo (Tech-Ready)

Requisitos Previos:

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

# Windows
```
- mvnw clean install
- mvnw spring-boot:run
```

# Linux / Mac

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
"id": 6,
"fecha": "2024-05-15",
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
"id": 2,
"nombre": "Masters 1000",
"puntos": 50,
"tipoDeTorneo": "M1000"
},
"superficie": "ARCILLA",
"fase": "R32",
"estado": "RETIRO",
"resultado": "6-3 1-0 RET",
"ganador": "Carlos Alcaraz"
}
```

```
Caso C para partido común de ATP, pero con un Tie-break

{
"id": 7,
"fecha": "2023-05-15",
"torneo": "Masters 1000 Roma",
"pais": "IT",
"ciudad": "Roma",
"jugador1": {
"id": 3,
"nombre": "Sebastián Báez",
"nacionalidad": "AR",
"rankingActual": 52
},
"jugador2": {
"id": 4,
"nombre": "Francisco Cerúndolo",
"nacionalidad": "AR",
"rankingActual": 19
},
"categoria": {
"id": 2,
"nombre": "Masters 1000",
"puntos": 50,
"tipoDeTorneo": "M1000"
},
"superficie": "ARCILLA",
"fase": "R32",
"estado": "FINALIZADO",
"resultado": "6-4 7-6",
"ganador": "Sebastián Báez"
}
```

## 🧪 Estrategia de QA:
Se definieron estratégicamente tres casos de prueba para abarcar distintas complejidades del dominio:

- Un partido al mejor de 5 sets (Grand Slam).
- Un escenario atípico de retiro ("RET") para validar las reglas de negocio en resultados incompletos.
- Un partido estándar al mejor de 3 sets (ATP).

## 🎯 Valor del proyecto
Este sistema demuestra la capacidad de diseñar integraciones robustas en un contexto similar al de sistemas empresariales reales, asegurando:

- Calidad de datos
- Tolerancia a fallos
- Consistencia entre sistemas


## 🎯 Enfoque
Desarrollo backend orientado a integraciones, con foco en:

- Calidad de datos.
- Resiliencia ante fallos.
- Consistencia entre sistemas desacoplados.

## 🔄 Enfoque de desarrollo

El proyecto fue desarrollado de manera iterativa, simulando un flujo de trabajo ágil:

- Implementación incremental de funcionalidades
- Validación continua mediante pruebas manuales (Postman)
- Ajustes progresivos a base de resultados y errores detectados
- Priorización de integraciones críticas (Salesforce, persistencia)

💡 Si bien se trata de un proyecto individual, se siguieron principios de desarrollo iterativo alineados a metodologías ágiles.

## 💻🧠 Sobre este proyecto
Este proyecto fue desarrollado de forma individual con el objetivo de simular un entorno real de integración empresarial, aplicando buenas prácticas de diseño, resiliencia y calidad de datos en un contexto cercano a producción.

## 🔮 Posibles mejoras

- Implementación de colas (RabbitMQ/Kafka) para manejo de reintentos
- Mayor cobertura de tests (unitarios e integración)
- Normalización del resultado de partidos en una estructura de sets
- Implementación de jobs batch para sincronización diferida