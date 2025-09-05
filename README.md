# TOXISAFE — Puesta en marcha

Aplicación de escritorio para la investigación y gestión de brotes de toxiinfección alimentaria (TIA).

## Requisitos

- **Windows 10/11**
- **JavaFX 21** — descarga oficial: <https://jdk.java.net/javafx21/>

> Este README asume que JavaFX se instalará en la ruta indicada más abajo.

## Instalación

1. **Descarga JavaFX 21** desde <https://jdk.java.net/javafx21/> (paquete `javafx-sdk-21.0.2` para Windows).
2. **Descomprime** el archivo descargado.
3. **Copia** la carpeta `lib` del SDK a la siguiente ubicación exacta (créala si no existe):

`C:\Program Files\Java\javafx-sdk-21.0.2\lib`

Al terminar, los `.jar` de JavaFX deben estar dentro de esa carpeta `lib`.

> Si usas otra ruta distinta, edita después `TOXISAFE.bat` y cambia el valor de `--module-path` para que apunte a tu carpeta `lib`.

## Ejecución

Tras completar la instalación anterior, **ejecuta**:

`TOXISAFE.bat`

Puedes hacerlo con doble clic o desde una consola (`cmd`) abierta en la carpeta de la aplicación.

## Datos de prueba y credenciales

La aplicación incluye una **base de datos por defecto** con:

- Datos de ejemplo para pruebas.
- **Síntomas** precargados.
- **Catálogo de alimentos** inicial.

Usuario administrador de demostración:

- **Usuario:** `admin`  
- **Contraseña:** `admin1`

> Por seguridad, cambia la contraseña del administrador tras el primer inicio de sesión.

## Problemas frecuentes

- **`Module javafx.controls not found`**  
  Verifica que JavaFX está en `C:\Program Files\Java\javafx-sdk-21.0.2\lib`.  
  Si usas otra ruta, edita `TOXISAFE.bat` y actualiza `--module-path`.

- **No arranca con doble clic**  
  Ejecuta `TOXISAFE.bat` desde `cmd` para ver el mensaje de error y corregir la ruta de JavaFX si fuese necesario.
