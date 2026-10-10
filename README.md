# eBye

Tienda Multi-comercio del proyecto de Sistemas Distribuidos (Tarea 3). Maneja productos y stock. Se comunica con **PedidosMasTarde** por paso de mensajes sobre sockets TCP/UDP.

> Estado actual: solo está implementado el servicio **Consultar existencia de productos** (UDP). Los demás servicios se irán agregando.

## Stack

- Java 1.8
- Apache Maven (fat JAR con `maven-shade-plugin`)
- PostgreSQL 12+
- Sockets UDP
- JSON via `json-simple 1.1.1`
- Driver JDBC `postgresql 42.7.4`

## Prerrequisitos

- JDK 1.8 o superior instalado y disponible en `PATH`
- Maven 3.6 o superior
- PostgreSQL 12 o superior corriendo en `localhost:5432`

## Setup

### 1. Crear la base de datos

Desde `psql` (o pgAdmin):

```sql
CREATE DATABASE ebye;
```

Después, cargar la tabla y los datos de ejemplo:

```bash
psql -U postgres -d ebye -f schema.sql
```

Si tu usuario o contraseña de PostgreSQL no son `postgres` / `postgres`, cambialos en `src/main/java/fpuna/ebye/bd/Bd.java`.

### 2. Compilar y empaquetar

```bash
mvn clean package
```

Genera un fat JAR ejecutable en `target/ebye-1.0-SNAPSHOT.jar`.

### 3. Ejecutar

```bash
java -jar target/ebye-1.0-SNAPSHOT.jar
```

Escucha en el puerto UDP `6000`. Para usar otro puerto, pasarlo como argumento:

```bash
java -jar target/ebye-1.0-SNAPSHOT.jar 6001
```

> Cada vez que cambies el código (por ejemplo la contraseña en `Bd.java`), hay que volver a correr `mvn clean package` y reiniciar el servidor.

## Estructura del proyecto

```
eBye-distri/
├── pom.xml                               Configuración de Maven y dependencias
├── schema.sql                            Tabla producto y datos de ejemplo
├── docs/eBye.md                          Contrato JSON de los servicios
└── src/main/java/fpuna/ebye/
    ├── Main.java                         Arranca el servidor UDP
    ├── server/UDPServer.java             Recibe datagramas y arma las respuestas
    ├── servicio/StockServicio.java       Lógica del servicio de stock
    ├── bd/Bd.java                        Conexión a PostgreSQL
    ├── bd/ProductoDAO.java               Consultas SQL de producto
    └── entidad/Producto.java             Entidad producto
```

## Comunicación distribuida

- **UDP**: Consultar existencia de productos (lo consume PedidosMasTarde).

El contrato JSON del servicio está en [`docs/eBye.md`](docs/eBye.md).
