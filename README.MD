## 🧩 Descripción del proyecto

Este proyecto consiste en un sistema de seguimiento de resultados de tenis diseñado como un puente de datos entre una API externa y Salesforce.

Su objetivo principal es recibir resultados de partidos profesionales, procesarlos y sincronizarlos automáticamente con Salesforce, garantizando consistencia, disponibilidad y calidad de la información en todo momento.

El sistema implementa una estrategia de persistencia intermedia: Los datos se almacenan primero en una base local y luego se sincronizan con la nube, asegurando robustez ante fallos externos.

## 💡 ¿Qué problemas resuelve?

## ✅ Eliminación de datos duplicados

Se implementa el uso  de External IDs como identificadores únicos. Esto permite:

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

API Tenis ➡️ Spring Boot ➡️ PostgreSQL ➡️ Spring Boot ➡️ Salesforce ➡️ SOAP API


- Entrada: API externa de resultados.

- Procesamiento: Backend en Spring Boot.

- Persistencia: Base de datos local (PostgreSQL).

- Salida: Salesforce (REST API).

- Enriquecimiento: Servicio externo vía SOAP.


## 🛠️ Tecnologías utilizadas:

-Java + Spring Boot:
Motor principal del sistema, encargado de la lógica de negocio y procesamiento de datos.

-PostgreSQL + Hibernate:
Persistencia local robusta y fácilmente desplegable.

-Salesforce API (REST):
Destino final donde los datos se convierten en registros utilizables a nivel empresarial.

-Maven:
Gestión de dependencias y generación de código (incluyendo clientes SOAP).

-Thunder Client:
Testeo de endpoints y validación del flujo de datos.


## 🔌 Integraciones

## 🌐 REST (JSON)

Utilizado para la comunicación con Salesforce:

- Ligero y eficiente.

- Nativamente soportado por el CRM.

- Ideal para operaciones CRUD de registros.

## 🧾 SOAP (XML)

El servicio SOAP recibe un código de país (ISO) y retorna la capital correspondiente.

Esta información se utiliza para enriquecer los datos antes de persistirlos en Salesforce.

Funcionalidad implementada:
A partir de un código de país (ej: AR), el sistema obtiene automáticamente su capital.

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

## 🚧 Desafíos técnicos abordados:

- Prevención de duplicados en integraciones externas
  → Resuelto mediante uso de External IDs

- Tolerancia a fallos en servicios externos (Salesforce)
  → Implementado mediante persistencia local intermedia

- Integración de SOAP en una arquitectura moderna REST
  → Adaptación mediante generación de clientes y procesamiento XML

- Consistencia entre sistemas desacoplados
  → Estrategia de sincronización controlada


## ⚙️ Guía de Instalación y Testeo (Tech-Ready)

Requisitos Previos:

- Java 17 instalado.

- PostgreSQL: Motor de base de datos instalado y corriendo localmente.

- Maven Wrapper: Incluido en el proyecto (no requiere instalación de Maven global).

Antes de iniciar la aplicación, asegurate de tener creada una base de datos en PostgreSQL.
Puede ser igual a la que tengo, llamada: "tenis_stats_db"

Podés configurar las credenciales en src/main/resources/application.properties:

- spring.datasource.url=jdbc:postgresql:"Tu servidor de tu base de datos creada"
- spring.datasource.username="Tu usuario"
- spring.datasource.password="Tu contraseña"

# Configuración de Hibernate para creación automática de tablas:
- spring.jpa.hibernate.ddl-auto=update
- spring.jpa.show-sql=true

De ser necesario, desde la terminal en la raíz del proyecto, ejecutá:

# Windows

- mvnw clean install
- mvnw spring-boot:run

# Linux / Mac

- ./mvnw clean install
- ./mvnw spring-boot:run

## O en intellij:

Hace click en el icono de Maven(A la derecha de la pantalla), posicionate en el menu despeglable de tenis.stats, después hace doble click en clean, luego install y luego run.

# Configuración en Salesforce

Crear un objeto llamado: Partido__c.

Configurar las credenciales con lo siguiente:
- salesforce.client.id=tu_id_del_cliente.
- salesforce.username=tu_usuario.
- salesforce.login.url=https://login.salesforce.com.
- salesforce.instance.url=https://tu-instancia.my.salesforce.com.
- salesforce.partido.endpoint=/services/data/v60.0/sobjects/Partido__c.

Una vez que hiciste todos esos pasos de configuraciones e instalaciones y una vez que la app esté corriendo en el puerto 8080, usa Thunder Client para realizar un:

POST http://localhost:8080/api/partidos/registrar
Permite registrar un partido, procesar su resultado y sincronizarlo con Salesforce.


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
Caso C para partido comùn de ATP, pero con un Tie-break

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

## Estrategia de QA:
Se definieron estratégicamente tres casos de prueba para abarcar distintas complejidades del dominio:
- Un partido al mejor de 5 sets (Grand Slam).
- Un escenario atípico de retiro ("RET") para validar las reglas de negocio en resultados incompletos.
- Un partido estándar al mejor de 3 sets (ATP).

## 🎯 Valor del proyecto
Este sistema demuestra la capacidad de diseñar integraciones robustas, asegurando calidad de datos, tolerancia a fallos y consistencia entre sistemas desacoplados.


## 🎯 Enfoque
Desarrollo backend orientado a integraciones, con foco en:

- Calidad de datos.
- Resiliencia ante fallos.
- Consistencia entre sistemas desacoplados.

## 💻🧠  Sobre este proyecto

Este proyecto fue desarrollado de forma individual con el objetivo de simular un entorno real de integración empresarial, aplicando buenas prácticas de diseño, resiliencia y calidad de datos.

