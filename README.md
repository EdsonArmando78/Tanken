# Tanken

Aplicación Android de asistencia al bienestar estudiantil en CUCEI. Funciona con
los sensores del teléfono; la integración con sensores vitales de un wearable es
una ampliación futura. No realiza diagnósticos.

El [plan de implementación y ejecución](docs/PLAN_IMPLEMENTACION.md) registra el
estado real del código, las tareas pendientes y los criterios para cerrar el
proyecto integrador. La presencia de una función en el reporte no acredita su
implementación: consultar la matriz de estado del plan.

## Entorno

- Android Studio y Android SDK; configurar la ubicación local del SDK sin subir
  `local.properties` a Git.
- JDK 25 para el daemon de Gradle, según `gradle/gradle-daemon-jvm.properties`.
  El código Java del módulo usa compatibilidad Java 11.
- Gradle 9.6.0 y Android Gradle Plugin 9.5.1 mediante los archivos del proyecto.
- `minSdk 26` (Android 8.0), `targetSdk 37`, `compileSdk 37`.
- Dispositivo previsto para la validación final: moto edge 50 pro con Android 16.
  Ese ensayo no demuestra compatibilidad con todos los dispositivos Android 8+.

## Compilar y verificar

Desde la raíz del repositorio, en Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
```

En Linux o macOS, usar `./gradlew` con las mismas tareas y un SDK local válido.
El APK debug se genera en `app/build/outputs/apk/debug/app-debug.apk`.

Las pruebas de `AcademicScheduleTest` y `WellnessSimulationTest` verifican lógica
de horario y simulación. `ClassTimersTest` y `WellnessThreadsTest` requieren un
dispositivo Android. Para ejecutarlas con un único teléfono de prueba conectado:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Los informes de tareas y pruebas se generan dentro de `app/build/reports/`.
Compilar el APK de pruebas no equivale a ejecutarlas. Consultar el plan para la
matriz manual, las limitaciones actuales y los resultados que aún deben obtenerse.

Validación del 18/11/2026 tras corregir lint: 14 pruebas unitarias aprobadas,
APK debug y APK de pruebas compilados, lint sin errores ni advertencias. No había
un teléfono conectado para ejecutar las pruebas instrumentadas. El arranque ahora
abre directamente `MainActivity`, sin la pantalla adicional con espera artificial.

Tras implementar las fases 2 y 3: 27 pruebas unitarias aprobadas, ambos APK
generados y lint sin incidencias. Las pruebas instrumentadas y la revisión
visual en el teléfono siguen pendientes.

Fases 4 y 5: 31 pruebas unitarias aprobadas; migración Room v1→v2 y nuevas
pruebas instrumentadas preparadas. La validación física sigue pendiente.

## Instalar en un teléfono físico

Activar depuración USB y autorizar el equipo en el teléfono. Los instaladores
buscan ADB en `PATH`, variables del SDK y la ubicación habitual del sistema.

```powershell
.\scripts\install-test-apk.cmd --list
.\scripts\install-test-apk.cmd
.\scripts\install-test-apk.cmd --serial SERIAL
.\scripts\install-test-apk.cmd --skip-build --no-launch
```

En Linux:

```sh
bash scripts/install-test-apk.sh --list
bash scripts/install-test-apk.sh
```

En Windows el lanzador `.cmd` utiliza `install-test-apk.ps1`. Los scripts resuelven
la raíz a partir de su propia ubicación y se pueden invocar mediante una ruta
absoluta desde otro directorio. Si hay varios teléfonos listos, es obligatorio
seleccionar uno con `--serial`. No se inicia un emulador automáticamente.

## Qué funciona actualmente

- Reloj y contexto académico: clase actual, próxima clase y tiempos restantes.
- Horario semanal: altas, edición, eliminación y cruces; persistencia Room con
  importación del horario anterior sin borrar sus preferencias originales.
- Movimiento: acelerómetro XYZ compartido mientras la app está visible; sesión
  conservada al navegar/rotar, pausa persistente y resúmenes en Room. Distingue
  lectura ausente/antigua del reposo observado; suspende en segundo plano.
- Temporizadores: hasta cuatro activos y cola adicional, cancelación y aviso al
  terminar. Se conservan al rotar; se cancelan al cerrar su pantalla y no sobreviven
  a la muerte del proceso. No son alarmas persistentes.
- Simulador: compara intervalos fijos y contexto académico sobre ocho horas del
  horario guardado, con entradas sintéticas y cancelación. Conserva ID,
  vencimiento original, reprogramaciones, emisiones y pendientes. No programa
  notificaciones reales. Guarda comparaciones completadas con sus entradas, horario
  y resultados para consultarlas o reproducirlas desde Historial.

- Recordatorios: hidratación, pausas y próximas clases, con días y silencio
  compartidos; acciones Realizado/Posponer 10 min/Omitir y recuperación persistente.
- Historial: filtros por fecha/tipo, detalles y borrado confirmado. El borrado
  completo cancela pendientes y desactiva recordatorios; el filtrado los conserva.
- Respiración: dos minutos de ciclos visuales 4/4; conserva avance al rotar y se
  pausa al salir.

Las alarmas son flexibles: Android puede retrasarlas. El cierre normal del proceso
conserva eventos; después de una detención forzada hay que abrir la app para
reactivar la programación. No se solicita acceso a alarmas exactas.

El monitor usa un filtro de gravedad de 0.5 s y umbrales demostrativos de 0.8 y
4 m/s². Cambiar la inclinación puede producir detecciones transitorias; no mide
pasos, pulso ni estados de salud. La ausencia de lecturas no acredita inactividad.

La comparación corre en `HandlerThread("HiloFondo")` y comunica progreso y resultado
con `Handler` y estados observables, sin esperas artificiales. El tiempo de cálculo
no equivale a la latencia de una notificación.

Las pantallas principales y las nuevas usan Material 3 Expressive con colores dinámicos,
Views XML y View Binding. El horario usa RecyclerView/ListAdapter/DiffUtil;
ViewModel/LiveData separan estado e interfaz. Room conserva hasta 30 días, con
límites por tipo y sin eliminar avisos pendientes. La base está excluida de respaldos.
Consultar [pendientes de integración](docs/PENDIENTES_INTEGRACION.md).

## Servicios, navegación y datos

Inicio, Recordatorios, Historial y Servicios usan navegación inferior. Ajustes
permite administrar lectura y notificaciones, o restablecer la app con confirmación:
borra horario/historial/preferencias, cancela avisos y restaura el catálogo inicial.
Conserva los permisos concedidos por Android. Room v3 migra los datos anteriores.
El logo de `Logo.png` se usa en el lanzador y Acerca de.

El directorio funciona sin conexión. Datos iniciales revisados el 18/11/2026 en la
[Unidad de Atención para la Salud del CUCEI](https://www.cucei.udg.mx/servicios/medicos-y-clinicos).
El horario publicado corresponde a la unidad; confirmar disponibilidad de cada servicio.
Desde Servicios se importa JSON UTF-8 de hasta 512 KiB, con confirmación de sustitución
completa. Un error conserva el catálogo vigente. Ejemplos:
[catálogo válido](docs/catalogo_valido.json) y [duplicado inválido](docs/catalogo_invalido.json).
Formato: `schemaVersion: 1`, `version` y entre 1 y 100 `services`; cada servicio requiere
`id` único, `name`, `location`, `source` HTTPS y `reviewedAt` ISO; `phone`, `extension`,
`email` y `hours` son opcionales. Los enlaces se abren externamente.

Fases 6 y 7: 31 unitarias aprobadas; APK de aplicación e instrumentadas compilados.
Pruebas en teléfono pendientes, incluida la migración v2→v3 y el restablecimiento.

## Organización

```text
app/src/main/         Código Java, manifiesto y recursos Android
app/src/test/         Pruebas de lógica independientes de Android
app/src/androidTest/  Pruebas en dispositivo
docs/                Plan de implementación y seguimiento
scripts/             Instalación del APK en Windows y Linux
tools/               Utilidades del repositorio
gradle/              Wrapper y versiones de herramientas
```

Los documentos de trabajo del equipo y la consigna se conservan en la raíz.
`Tanken_PF_corregido.docx` es la referencia de alcance acordada; sus secciones de
desarrollo, pruebas, resultados y conclusiones se completarán con evidencia real.

## Archivo ZIP de fuentes rastreadas

```sh
python tools/zip-tracked.py
```

Esta utilidad incluye el contenido actual de los archivos rastreados por Git y
omite los eliminados. No incluye archivos nuevos sin rastrear, como los documentos
de trabajo o los scripts recién movidos hasta que se incorporen a Git. No sustituye
el paquete final con reporte, APK y evidencias descrito en el plan. No sobrescribe
un ZIP existente.
