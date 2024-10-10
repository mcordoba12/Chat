# Sistema de Chat con Audio y Llamadas

- Angela Gonzalez
- Juiana Filigrana
- Felipe Rivas
- Corina

## Descripción General

Este proyecto implementa un sistema de chat en Java que permite a múltiples usuarios comunicarse a través de mensajes de texto, mensajes de audio y llamadas de voz en tiempo real. El sistema consta de una aplicación cliente con interfaz gráfica y un servidor que gestiona las conexiones y la comunicación entre clientes.

## Características Detalladas

1. **Mensajería de texto en tiempo real**
   - Mensajes públicos visibles para todos los usuarios
   - Mensajes privados entre usuarios específicos
2. **Comunicación por audio**
   - Grabación y envío de mensajes de audio
   - Reproducción de mensajes de audio recibidos
3. **Llamadas de voz**
   - Establecimiento de llamadas de voz entre dos usuarios
   - Transmisión de audio en tiempo real durante las llamadas
4. **Interfaz gráfica de usuario (GUI)**
   - Cliente: Interfaz intuitiva para interacción del usuario
   - Servidor: Panel de control para monitoreo de actividades
5. **Gestión de usuarios**
   - Registro de nuevos usuarios con nombres únicos
   - Notificaciones de conexión y desconexión de usuarios

## Estructura del Proyecto

### Cliente

1. **Client.java**
   - Propósito: Clase principal del cliente con GUI y lógica de conexión
   - Funciones clave:
     - Conexión al servidor (localhost:6789 por defecto)
     - Interfaz gráfica para interacción del usuario
     - Envío y recepción de mensajes de texto y audio
     - Inicio y manejo de llamadas de voz
     - Gestión de la sesión del usuario

2. **AudioRecorder.java**
   - Propósito: Manejo de la funcionalidad de grabación de audio
   - Funciones clave:
     - Configuración del formato de audio (16kHz, 16 bits, estéreo)
     - Grabación de audio desde el micrófono
     - Conversión de audio a formato transmisible

3. **Lector.java**
   - Propósito: Lectura asíncrona de mensajes del servidor
   - Funciones clave:
     - Ejecución en un hilo separado
     - Recepción continua de mensajes del servidor

4. **AudioFormatTest.java**
   - Propósito: Utilidad para probar formatos de audio soportados
   - Funciones clave:
     - Listado de formatos de audio soportados por el sistema

### Servidor

(La estructura del servidor se mantiene como se describió anteriormente)

## Funcionamiento Detallado

### Inicio del Sistema

1. **Servidor**:
   - Ejecución: `java Server`
   - Inicia la GUI del servidor
   - Comienza a escuchar conexiones en el puerto 6789

2. **Cliente**:
   - Ejecución: `java Client`
   - Intenta conectar al servidor en localhost:6789
   - Solicita al usuario ingresar un nombre

### Proceso de Conexión

(Se mantiene como se describió anteriormente)

### Envío de Mensajes

1. **Mensajes de Texto**:
   - Cliente: El usuario escribe el mensaje y presiona "Enviar"
   - La interfaz muestra el mensaje en un "globo" rosa
   - El mensaje se envía al servidor para su difusión

2. **Mensajes de Audio**:
   - Cliente:
     - Usuario presiona "Grabar Audio"
     - AudioRecorder captura el audio
     - Al detener, se convierte a Base64 y se envía
   - La interfaz muestra un botón de reproducción para el audio
   - El servidor recibe y reenvía el audio a otros clientes

### Llamadas de Voz

1. **Inicio de Llamada**:
   - Cliente:
     - Usuario presiona "Llamar"
     - Ingresa el nombre del destinatario
     - Envía solicitud "CALL:nombreUsuario" al servidor
   - Servidor:
     - Crea un nuevo ServerSocket para la llamada
     - Envía el puerto de la llamada al destinatario

2. **Establecimiento de la Llamada**:
   - Cliente destinatario:
     - Recibe "CALLPORT:puerto" del servidor
     - Establece conexión con el socket de audio
   - Ambos clientes:
     - Inician AudioCallHandler para manejar la transmisión de audio

3. **Transmisión de Audio**:
   - AudioCallHandler:
     - Inicia dos hilos: uno para enviar audio del micrófono, otro para reproducir audio recibido
     - Utiliza TargetDataLine para capturar audio del micrófono
     - Utiliza SourceDataLine para reproducir audio recibido

4. **Finalización de la Llamada**:
   - Usuario presiona "Colgar"
   - Se cierran los sockets de audio
   - La interfaz vuelve al estado normal de chat

### Reproducción de Mensajes de Audio

- Al recibir un mensaje de audio, se muestra un botón de reproducción
- Al presionar el botón, se decodifica el audio Base64 y se reproduce

### Desconexión

1. Cliente presiona "Desconectar" o cierra la ventana
2. Se cierran todos los sockets (principal y de audio)
3. Se notifica al servidor
4. La interfaz del cliente se cierra

## Configuración y Ejecución

### Requisitos
- Java Runtime Environment (JRE) 8+
- Micrófono y altavoces/auriculares para funcionalidad de audio y llamadas

## Instrucciones de Ejecución

Siga estos pasos para ejecutar el sistema de chat:

### Requisitos Previos

1. Asegúrese de tener instalado Java Development Kit (JDK) 8 o superior en su sistema.
2. Verifique que tiene un micrófono y altavoces o auriculares funcionando correctamente en su computadora.

### Pasos para Ejecutar el Programa

1. **Compilación del Código**
   
   Abra una terminal o línea de comandos y navegue hasta el directorio que contiene los archivos `.java`. Luego, ejecute el siguiente comando para compilar todos los archivos:

   ```
   javac *.java
   ```

   Esto creará los archivos `.class` necesarios para ejecutar el programa.

2. **Iniciar el Servidor**
   
   Una vez compilado el código, inicie el servidor con el siguiente comando:

   ```
   java Server
   ```

   Debería ver una ventana rosa que indica que el servidor está en funcionamiento y esperando conexiones.

3. **Iniciar el Cliente**
   
   Para cada cliente que desee conectar, abra una nueva terminal o línea de comandos y ejecute:

   ```
   java Client
   ```

   Se abrirá una ventana de chat para cada cliente.

4. **Conexión del Cliente**
   
   - En la ventana del cliente, se le pedirá que ingrese un nombre de usuario.
   - Introduzca un nombre y presione Enter.
   - Si el nombre es válido, se conectará al chat.

5. **Uso del Chat**
   
   - Para enviar un mensaje de texto, escriba en el campo de texto y presione "Enviar Mensaje" o Enter.
   - Para grabar un mensaje de audio, presione "Grabar Audio", hable, y luego presione "Detener Grabación".
   - Para iniciar una llamada, presione "Llamar" e ingrese el nombre del usuario al que desea llamar.
   - Para colgar una llamada en curso, presione el botón "Colgar".

6. **Desconexión**
   
   Para desconectarse, simplemente cierre la ventana del cliente o presione el botón "Desconectar".

### Notas Importantes

- Asegúrese de que el servidor esté en ejecución antes de intentar conectar cualquier cliente.
- El servidor está configurado para ejecutarse en `localhost` (127.0.0.1) en el puerto 6789. Si necesita cambiar esto, modifique las constantes `SERVER_IP` y `PORT` en la clase `Client.java`.
- Para probar la funcionalidad completa, se recomienda ejecutar al menos dos instancias de cliente en diferentes terminales.

### Solución de Problemas

- Si encuentra problemas con el audio, asegúrese de que su micrófono y altavoces estén correctamente configurados en su sistema operativo.
- Si el cliente no puede conectarse al servidor, verifique que el servidor esté en ejecución y que no haya un firewall bloqueando la conexión.
- En caso de errores durante la compilación, asegúrese de tener la versión correcta de Java instalada y que todos los archivos `.java` estén en el mismo directorio.
