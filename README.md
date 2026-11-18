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
  y resultados para reproducirlas; la pantalla de historial queda pendiente.

El monitor usa un filtro de gravedad de 0.5 s y umbrales demostrativos de 0.8 y
4 m/s². Cambiar la inclinación puede producir detecciones transitorias; no mide
pasos, pulso ni estados de salud. La ausencia de lecturas no acredita inactividad.

La comparación corre en `HandlerThread("HiloFondo")` y comunica progreso y resultado
con `Handler` y estados observables. Actualmente incluye tres segundos de trabajo
simulado. Ese tiempo no es la latencia real de una notificación.

Inicio, Horario y Movimiento usan Material 3 Expressive con colores dinámicos,
Views XML y View Binding. El horario usa RecyclerView/ListAdapter/DiffUtil;
ViewModel/LiveData separan estado e interfaz. Room conserva hasta 30 días, con
límites por tipo y sin eliminar avisos pendientes. La base está excluida de respaldos.
Consultar [pendientes de integración](docs/PENDIENTES_INTEGRACION.md).

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
