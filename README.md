# Tanken

Aplicación Android de asistencia al bienestar estudiantil en CUCEI. Integra
horario de clases, recordatorios, monitor de movimiento, historial, respiración
guiada y directorio de servicios. Funciona con sensores del teléfono y datos
locales; la lectura de sensores vitales de un wearable es trabajo futuro.
No realiza diagnósticos.

## Compilar

Requiere Android SDK 37 y JDK 25 para Gradle. Configurar el SDK mediante
`local.properties` sin incorporarlo a Git. El código Java usa compatibilidad 11.
El proyecto incluye Gradle 9.6.0 y Android Gradle Plugin 9.5.1.
Android mínimo: 8.0 (API 26); compileSdk y targetSdk: 37.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
```

En Linux o macOS, usar `./gradlew` con las mismas tareas.
APK: `app/build/outputs/apk/debug/app-debug.apk`.
Informes: `app/build/reports/`.

Validación del 18/11/2026: 34 pruebas unitarias aprobadas, APK debug y de pruebas
compilados y lint sin incidencias. Las pruebas instrumentadas no se ejecutaron.
Las correcciones de filtros, selección de días, respiración y permisos de
notificaciones se comprobaron manualmente en un moto edge 50 pro con Android 16.
La entrega de avisos tras reiniciar el teléfono conserva un resultado inconcluso.

Para ejecutar pruebas instrumentadas con un dispositivo conectado:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Instalar

Activar depuración USB y autorizar el equipo en el teléfono.

```powershell
.\scripts\install-test-apk.cmd --list
.\scripts\install-test-apk.cmd
.\scripts\install-test-apk.cmd --serial SERIAL
.\scripts\install-test-apk.cmd --skip-build --no-launch
```

En Linux: `bash scripts/install-test-apk.sh`. Los scripts buscan ADB en el
entorno y en las ubicaciones habituales del SDK. Con varios dispositivos,
seleccionar uno mediante `--serial`.

## Funcionamiento

- Horario semanal con edición, eliminación y rechazo de empalmes; persistencia Room.
- Recordatorios de hidratación, pausas y próximas clases, con días y silencio;
  acciones Realizado, Posponer diez minutos y Omitir. Se advierte si faltan permisos.
- Historial con filtros, detalles y borrado confirmado.
- Monitor de acelerómetro mientras la app está visible; conserva estado al girar
  y suspende lecturas en segundo plano. No mide pasos, pulso ni estados de salud.
- Respiración visual de dos minutos con ciclos 4/4; guarda el progreso al salir
  y permite retomarlo en pausa.
- Comparación de avisos fijos y con contexto académico sobre ocho horas del
  horario guardado. Guarda resultados; la simulación no programa avisos reales.
- Temporizadores con hasta cuatro activos y una cola adicional. Se conservan
  al girar, pero se cancelan al cerrar su pantalla y no sobreviven a la muerte
  del proceso.
- Directorio local de servicios con importación JSON y confirmación de sustitución.
  Un archivo inválido conserva los datos anteriores. Los enlaces abren otras apps.
- Tutorial, ayuda, tema del sistema y restablecimiento confirmado de datos locales.

Las alarmas son flexibles y Android puede retrasarlas. Tras una detención forzada
hay que abrir la app para reactivar la programación. No se solicita acceso a
alarmas exactas. El movimiento es una estimación demostrativa: la falta de
lecturas no acredita reposo y los cambios de inclinación pueden producir
detecciones transitorias.

## Catálogos de prueba

[Catálogo válido](tools/catalogs/catalogo_valido.json) y
[catálogo inválido](tools/catalogs/catalogo_invalido.json).
Importar desde Servicios; tamaño máximo de 512 KiB, UTF-8.

Formato: `schemaVersion: 1`, `version` y entre 1 y 100 `services`. Cada servicio
requiere `id` único, `name`, `location`, `source` HTTPS y `reviewedAt` ISO.
`phone`, `extension`, `email` y `hours` son opcionales.

## Organización

```text
app/src/main/         Java, manifiesto y recursos Android
app/src/test/         Pruebas unitarias
app/src/androidTest/  Pruebas en dispositivo
app/schemas/         Esquemas Room para migraciones
scripts/             Instalación del APK
tools/catalogs/      Archivos de prueba de importación
tools/zip-tracked.py  Empaquetado de fuentes rastreadas
gradle/              Wrapper y versiones
```

`python tools/zip-tracked.py` crea un ZIP con el contenido actual de los archivos
rastreados por Git. Omite archivos eliminados y no incluye archivos nuevos sin
rastrear. No sobrescribe un ZIP existente.

## Licencia

Tanken se distribuye bajo [BSD de 3 cláusulas](LICENSE). La iconografía Material
Symbols Rounded conserva su licencia Apache 2.0. Licencias y atribuciones locales
en `app/src/main/assets/licenses/`.
