
# Descargar el código fuente

  - Ubicación del proyecto: https://gitlab.com/fmancia/sd/tree/master/lab-socket/ProyectoJava

  - Clonar Proyecto
      `git clone git@gitlab.com:fmancia/sd.git`
 ó bien
      `git clone https://gitlab.com/fmancia/sd.git`

  - Verificar que el directorio dentro de su disco: /lab-socket/ProyectoJava  (En ese directorio se encuentra la práctica de sockets tcp/upd)


# Instalar software base

  - 1. Instalar servidor de base de datos PostgreSQL: https://www.postgresql.org/

  - 2. Instalar el JDK de Java (para esta práctica es JDK1.8 o JDK8). https://www.oracle.com/java/technologies/downloads/#java8
  
    - 2.1. Asegurarse que este configurado la variable de entorno PATH con la carpeta "bin" correspondiente al JDK (no al JRE).

    - 2.2. Asegurarse que este configurado la variable de entorno JAVA_HOME apuntando a la carpeta de JDK sin incluir la subcarpeta "bin". Ej: 


  - 3. Instalar Maven: https://maven.apache.org/install.html 

    - 3.1. Asegurarse que este configurado la variable de entorno PATH con la carpeta correspondiente a maven.

  - 4. Instalar un IDE, Ejemplo: Visual Studio Code, Eclipse, etc


# Base de datos

  - Deberá crear una base de datos Postgresql con llamada “sd”
  - Deberá crear estructura cuyo script de creación está en el repositorio en el
directorio: “BaseDatos”.
  - Deberá configurar en la clase py.una.bd.Bd.java lo siguiente:
      - IP, puerto y nombre de la BD (variable url)
      - Usuario y Password del postgresql (variables user y password)


# UDP, Protocolo de Datagramas de Usuario (datagramas)
◦ Servidor UDP
▪ Ejecutar Clase servidor py.una.server.udp.UDPServer (Puede ejecutar en una
consola aparte o desde el IDE)
▪ Solo puede ejecutar uno a la vez.
▪ Intente ejecutar y se generará un error similar a java.net.BindException:
Address already in use: Cannot bind
◦ Cliente UDP
▪ Ejecutar Clase servidor py.una.server.udp.UDPClient (Puede ejecutar en una
consola aparte o desde el IDE)
▪ Puede Ejecutar varios clientes en simultáneo.

# TCP, Protocolo de Control de Transmisión (flujo)
◦ Servidor TCP, Una petición de un solo cliente.
◦ Servidor TCP, Varias peticiones de un solo cliente.
◦ Servidor TCP, Varias peticiones de varios clientes (Multi-hilo)


# Verificación con comando netstat:
 - Windows:

`netstat -a -b -p udp` 

`netstat -a -b -p tcp`
 - Unix/Linux:

`netstat -upnl`

`netstat -tpnl`



Fuente: https://grado.pol.una.py/pluginfile.php/286807/mod_resource/content/1/LAB-01_SOCKETS.pdf
