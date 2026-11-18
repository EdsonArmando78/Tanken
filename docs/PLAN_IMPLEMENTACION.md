# Plan de implementación y ejecución de Tanken

Fecha de preparación: 18 de noviembre de 2026.

Este documento organiza los trabajos para cerrar el código y obtener evidencia
del proyecto integrador. Parte del repositorio actual y del alcance acordado en
`Tanken_PF_corregido.docx`; no representa funciones ya terminadas ni pruebas ya
ejecutadas. Marcar tareas al completar sus criterios de aceptación.

## Alcance acordado

- Una aplicación Android autónoma para estudiantes del CUCEI desde Android 8.0
  (API 26). Validación física en moto edge 50 pro con Android 16.
- Movimiento con sensores del teléfono, horario editable, avisos de hidratación,
  pausas y próximas clases, historial local y preferencias.
- Respiración guiada sencilla e importación de catálogo de servicios de salud.
  Ambos extras se mantienen por decisión expresa del usuario.
- Comparación reproducible de programación fija y contextual, separada de la
  adquisición real y de las notificaciones del sistema.
- Operación local sin cuenta ni servidor. Los enlaces institucionales pueden
  necesitar Internet. Sin diagnósticos ni inferencias de estrés.
- Wearables y sensores vitales son ampliación futura. Una interfaz de lectura
  podrá admitir equipos con acceso autorizado y documentado sin exigir Wear OS.
  No implementar BLE, app para reloj ni SDK de fabricantes en esta entrega;
  tampoco prometer acceso universal por conceder permisos.

El cronograma académico conserva el 16 de noviembre como cierre planeado y el 18
como fecha de entrega. Registrar las fechas reales de ejecución y correcciones.

## Estado comprobado en el código

| Área | Implementación existente | Trabajo pendiente |
| --- | --- | --- |
| Inicio | Jornada, movimiento y próximo aviso; cuatro pestañas con Navigation | Validación física de navegación. |
| Horario | Room, importación del JSON anterior, edición, cruces, ViewModel y ListAdapter | Integrar el futuro motor de avisos. |
| Sensores | Fuente compartida de primer plano, filtro, estados, pausa y resúmenes Room | Validación física de lectura y ciclo de vida. |
| Temporizadores | `ClassTimersViewModel`, `TimersActivity`, `TimerNotifications` | Mantener herramienta independiente; no confundirla con alarmas persistentes. |
| Comparación | XML Expressive, resultados guardados y reproducibles; sin esperas artificiales | Validación física de cancelación y reproducción. |
| Bienestar real | Coordinador persistente, alarmas flexibles, preferencias y acciones sobre IDs estables | Validación física de avisos y recuperación. |
| Historial | Room v3, filtros por fecha/tipo, detalles y dos modalidades de borrado confirmado | Validación física de filtros y borrado. |
| Respiración | Sesión visual 4/4, pausa/detención y ViewModel; pausa al salir | Validación física de animación y rotación. |
| Servicios | Directorio local, detalle e importación JSON validada; Room v3 | Validación física de importación y enlaces. |
| Ajustes y privacidad | Permisos/canales, lectura, restablecimiento y exclusión de respaldos | Validación física del borrado y permisos. |
| Pruebas | 31 unitarias aprobadas; instrumentadas preparadas para migración, persistencia, cancelación y ciclo de vida | Ejecutar instrumentadas y recorrido físico. |

Configuración actual: Java 11 para el código, Gradle 9.6.0, AGP 9.5.1, daemon
JDK 25, `compileSdk 37`, `targetSdk 37`, `minSdk 26`. Reutilizar Java, XML,
AndroidX, Material, `java.time`, JSON y almacenamiento privado existentes.
Las versiones se actualizaron durante la limpieza de lint autorizada por el usuario.
No migrar a Kotlin/Compose. Por decisión posterior del usuario, usar Room para
horario y registros; SharedPreferences para preferencias sencillas. Incorporar
View Binding, RecyclerView + ListAdapter/DiffUtil y Lifecycle + ViewModel/LiveData
donde corresponda. Renovar primero Inicio, Horario y Movimiento con Material 3 Expressive.

## Organización y limpieza aplicada

- `README.md`: instalación, compilación y limitaciones vigentes.
- `docs/PLAN_IMPLEMENTACION.md`: guía de trabajo del integrador.
- `scripts/install-test-apk.{cmd,ps1,sh}`: instaladores reutilizables con rutas
  corregidas tras moverlos desde la raíz.
- `tools/zip-tracked.py`: utilidad de ZIP de fuentes rastreadas.
- Se consolidó el contenido útil de `PRACTICA_13.md` y `PRACTICA_14.md` en estas
  guías y se retiraron esos documentos de prácticas.
- Se retiraron `ExampleUnitTest` y `ExampleInstrumentedTest`, plantillas sin
  cobertura funcional del producto. Se conservan las pruebas específicas.
- Se ignoran bloqueos de Office, cachés Python y salidas `dist/`. No borrar un
  bloqueo mientras Word tiene el documento abierto.
- Se conservan reportes del equipo, consigna, configuración local, wrapper y
  cachés de desarrollo. No retirar recursos aún referenciados por la aplicación.

## Orden de ejecución

### 1. Confirmar la línea base

- [x] Ejecutar `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`
  y registrar comandos, fecha y resultados antes de cambios funcionales.
- [ ] Instalar en el teléfono autorizado y recorrer reloj, horario, movimiento,
  temporizadores y simulador. Registrar fallos reproducibles.
- [ ] Verificar que actualizar la app conserva el horario. No desinstalar ni
  borrar datos como solución rutinaria.

Criterio de salida: línea base conocida, separada de los resultados posteriores.
Las validaciones históricas de prácticas no acreditan una ejecución actual.

### 2. Definir persistencia y estados antes de pantallas nuevas

- [x] Añadir modelos pequeños para preferencias, evento de aviso, resumen de
  movimiento y resultado de comparación; usar repositorios y Room privados.
- [x] Guardar versión de formato, IDs estables y fecha. Serializar modificaciones
  desde una sola ruta y hacer E/S fuera del hilo principal. Acotar el historial.
- [x] Definir estados: programado, pospuesto, emitido, realizado, omitido y
  cancelado. Distinguir posposición por clase y por usuario.
- [x] Conservar vencimiento original y próximo intento separados. Una respuesta
  repetida no crea otro evento ni cambia un estado terminal.
- [x] Tolerar registros dañados sin cerrar la app ni reemplazar silenciosamente
  todo el historial por datos vacíos. El borrado exige confirmación.

Implementación realizada; las pruebas instrumentadas y la validación física siguen pendientes.

Criterio de salida: guardar/cargar, rotación, reapertura y respuestas repetidas
mantienen IDs, estados y horarios. Diferenciar lecturas y respuestas declaradas.

### 3. Completar horario y adquisición del teléfono

- [x] Añadir edición a `ScheduleActivity`, validando cruces contra otras clases;
  conservar la anterior al cancelar o rechazar el nuevo intervalo.
- [x] Extraer lectura y filtro de `MotionActivity` a un componente reutilizable
  si lo necesitan inicio y reglas; no construir una integración wearable completa.
- [x] Mantener XYZ, unidad, fecha, origen y disponibilidad. Diferenciar dato
  ausente/antiguo, reposo observado y movimiento observado.
- [x] Conservar sesión al rotar con ViewModel. Registrar y liberar listeners según
  ciclo de vida, evitando duplicados al regresar o pausar la lectura.
- [x] Guardar resúmenes acotados, no cada muestra. La entrega no requiere adquisición
  continua durante todo el día en segundo plano.

Decisiones: lectura compartida mientras la app está visible; suspensión con el ciclo
de vida del proceso. Resúmenes por minuto observado y parciales al pausar. Retención:
30 días, hasta 5.000 resúmenes, 2.000 avisos terminales y 100 comparaciones; conservar
avisos pendientes. UUID estables, fechas epoch-ms en Room y fechas ISO/zona en
instantáneas JSON. Base v1 con esquema exportado, sin recuperación destructiva.

Criterio de salida: el monitor responde al movimiento, informa ausencia de sensor
y conserva sesión al rotar. No atribuye inactividad a periodos sin lectura.

### 4. Implementar avisos reales y recuperación

- [x] Compartir reglas contextuales con `WellnessSimulation` sin modificar las
  condiciones controladas de su comparación.
- [x] Añadir configuración de hidratación, pausas y anticipación de clase:
  activación, intervalo, días y horario de silencio, con entradas validadas.
- [x] Hidratación depende del tiempo elegido; las pausas pueden considerar contexto
  y movimiento disponible. No contar periodos sin lectura como reposo comprobado.
- [x] Programar vencimientos fuera de una Activity, con eventos guardados y alarmas
  flexibles del sistema. No pedir alarmas exactas para bienestar ni mantener un
  hilo durmiendo como mecanismo de persistencia.
- [x] Al vencer, releer preferencias y horario; reevaluar clase, silencio,
  pertinencia y estado. Conservar el vencimiento original.
- [x] Publicar notificación y pantalla con Realizado/Posponer/Omitir sobre el mismo
  ID. Si se deniega el permiso, conservar el evento consultable en la aplicación.
- [x] Recuperar programación tras reinicio, cambio de hora/zona y modificación de
  horarios o preferencias. Cancelar lo que ya no corresponda; declarar los
  receptores y permisos necesarios en el manifiesto.
- [x] Reutilizar canales/utilidades cuando corresponda y mantener separados los
  temporizadores académicos ligados a pantalla.

Implementación realizada; queda la validación física de alarmas, permisos y recuperación.

Criterio de salida: los avisos no dependen de dejar su pantalla abierta ni se
duplican por recreación o entrega repetida. Registrar restricciones y retrasos de
Android; distinguir muerte del proceso de detención forzada por el usuario.

### 5. Cerrar historial, comparación y respiración

- [x] Filtrar historial por fecha/tipo, mostrando origen real/simulado, estado,
  vencimiento y respuesta. Confirmar borrado y tratar avisos pendientes de manera
  coherente con la opción elegida antes de eliminar datos.
- [x] Guardar entrada, horario usado, periodo, zona, modalidad y eventos de cada
  comparación para reproducir el resultado. Identificar siempre la simulación.
- [x] Conservar el caso sin emisiones como no calculable y comparar total emitido,
  retrasos y pendientes. No presentar la simulación como notificación real.
- [x] Retirar los tres segundos de espera artificial si ya no aportan al producto;
  adaptar las pruebas de hilos mediante tareas controlables, manteniendo
  cancelación y rechazo de resultados antiguos.
- [x] Implementar respiración visual sencilla por iniciativa del usuario, sin
  detectar estrés, prometer efectos clínicos ni añadir audio o bibliotecas.
  Detener callbacks al salir y definir pausa y rotación.

Decisiones: pausa por tiempo sin datos recientes; silencio para todos los avisos;
Posponer añade 10 minutos. Respiración de dos minutos, ciclos 4/4 sin retenciones.
El borrado filtrado conserva pendientes; el borrado completo los cancela y desactiva
recordatorios. Room v2 incorpora vigencia, revisión y datos de entrega sin borrar v1.

Criterio de salida: registros persistentes, fuentes distinguibles y respiración
que pueda detenerse sin dejar tareas ejecutándose.

### 6. Añadir servicios e importación del catálogo

- [x] Preparar JSON inicial de Medicina General, Psicología y Nutrición con datos
  verificados en publicaciones oficiales: ID, nombre, ubicación, contacto, horario
  cuando exista, fuente y fecha de revisión. No inventar contactos.
- [x] Mostrar directorio y detalle sin red; abrir enlaces externamente y controlar
  fallos por falta de conectividad o aplicación compatible.
- [x] Importar mediante el selector de documentos del sistema sin acceso general
  al almacenamiento. Limitar tamaño y validar versión, campos, IDs duplicados y
  enlaces antes de aplicar la actualización completa.
- [x] Conservar catálogo anterior ante error y mostrar versión/fuente/fecha.
  Preparar un ejemplo válido y otro inválido; sin descarga automática ni servidor.

Criterio de salida: importar cambios de contacto y ubicación, consultarlos sin red
y rechazar un archivo inválido conservando íntegro el catálogo vigente.

### 7. Integrar navegación, ajustes y privacidad

- [x] Organizar inicio con jornada, movimiento y próximo aviso, más accesos a
  horario, avisos, historial, respiración, servicios y comparación.
- [x] Hacer efectivos permisos, preferencias, pausa de lectura y borrado de datos.
  Configurar `backup_rules.xml` y `data_extraction_rules.xml` para excluir registros
  sensibles según las rutas reales del almacenamiento.
- [x] Preparar ciclo de vida, teclado, Atrás, estados vacíos y filtros en horizontal.
  Mantener textos y unidades en recursos; comprobar presentación en el teléfono.
- [x] Sustituir tutorial antiguo por ayuda del producto. Retirar calculadora,
  saludo de ejemplo, video y layouts antiguos solo después de retirar sus accesos
  y referencias en Java, XML y manifiesto.
- [x] Evitar rediseño masivo y migraciones; completar las rutas principales usando
  componentes existentes.

Criterio de salida: cada botón tiene una función real y no quedan referencias
rotas ni accesos a demostraciones ajenas al alcance.

### 8. Validar, reunir evidencia y cerrar la entrega

- [ ] Ejecutar comprobaciones automatizadas finales y corregir errores nuevos;
  registrar advertencias pendientes y su impacto.
- [ ] Ejecutar pruebas instrumentadas en el teléfono y la matriz manual siguiente.
  Registrar fecha, dispositivo, versión, pasos, esperado, obtenido y evidencia.
- [ ] Guardar capturas, video y resultados sin datos personales ajenos al proyecto.
- [ ] Completar desarrollo, funcionamiento, pruebas, resultados y conclusiones del
  Word con evidencia real; registrar evaluadores efectivamente participantes.
  La muestra inicial prevista son los dos integrantes.
- [ ] Sincronizar los requerimientos técnicos del Word con la configuración final:
  la limpieza de lint actualizó Gradle, AGP, AndroidX y el SDK objetivo.
- [ ] Preparar inventario de archivos Java, XML, recursos y configuración para
  explicar cada archivo incluido durante el video. Grabar funciones reales y
  errores controlados relevantes.
- [ ] Preparar ZIP/RAR con fuentes necesarias, wrapper, instrucciones, reporte,
  APK y evidencias. Excluir `.git`, `.gradle`, `.idea`, `local.properties`, cachés,
  bloqueos y compilaciones intermedias. Revisar el listado y comprobar que incluye
  reporte vigente, plan y archivos nuevos.

`tools/zip-tracked.py` solo archiva fuentes rastreadas; no usar su salida como
entrega completa sin agregar reporte y evidencias. No ejecutar `git add`, commit
o push implícitamente para solucionar esta diferencia.

## Matriz mínima de validación final

| Caso | Comportamiento que debe comprobarse |
| --- | --- |
| Horario vacío y clases consecutivas | Reloj utilizable, siguiente ocurrencia correcta, inicio inclusivo y fin exclusivo. |
| Alta, edición, borrado y cruces | Cambios persistidos; conflicto rechazado sin perder la clase previa. |
| Sensor quieto, movimiento y ausencia | XYZ y estado cambian; falta de sensor/datos informada sin inventar actividad. |
| Concurrencia y rotación | UI interactiva, sesión coherente, sin listeners ni callbacks duplicados. |
| Aviso durante clase y silencio | Posposición contextual conservando ID y vencimiento original. |
| Respuestas repetidas | Realizado/Posponer/Omitir producen transiciones coherentes sin duplicados. |
| Permiso/canal denegado | Restricción informada y evento consultable. |
| Cierre, reapertura y reinicio | Recuperación sin depender de una Activity abierta. |
| Cambio de hora/zona y horario | Recalcular sin duplicados ni configuración anterior. |
| Historial y borrado | Filtros, persistencia, origen y confirmación. |
| Respiración | Inicio, pausa/detención, rotación y salida sin tareas huérfanas. |
| Catálogo válido e inválido | Actualización completa o conservación de la versión anterior. |
| Sin Internet | Funciones locales disponibles; fallo controlado de enlaces externos. |
| Hipótesis | Mismas entradas y comparación de conteos, proporciones, retrasos y pendientes. |

Medir la meta de respuesta UI menor a un segundo desde la acción hasta su
representación, durante concurrencia en el teléfono. No usar las esperas
artificiales del simulador como evidencia de latencia ni de beneficios de salud.

## Registro de ejecución

Actualizar este registro durante el trabajo. Tener código escrito no acredita
comportamiento si falta compilar o ejecutar la prueba correspondiente.

| Fecha | Trabajo o comando | Resultado y evidencia | Pendiente |
| --- | --- | --- | --- |
| 18/11/2026 | Inspección y organización de documentación/scripts | Estado inicial descrito arriba; módulos nuevos pendientes. | Ejecutar etapas funcionales y registrar sus resultados. |
| 18/11/2026 | `gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`, con caché de usuario en `C:\Users\Edson\.gradle` | Compilación correcta; 14 pruebas unitarias, 0 fallos; APK debug y APK de pruebas generados; lint: 0 errores y 53 advertencias. Informes en `app/build/reports/` y resultados XML en `app/build/test-results/testDebugUnitTest/`. | Resolver advertencias pertinentes durante la integración; ejecutar pruebas instrumentadas en el teléfono. |
| 18/11/2026 | Ayuda de instaladores Windows/Linux desde otra carpeta, análisis de sintaxis PowerShell y `bash -n` | Correctos tras la reubicación. No se instaló ni abrió la app como parte de estas verificaciones. | Instalación y recorrido físico de la etapa 1. |
| 18/11/2026 | `tools/zip-tracked.py` con salida temporal y revisión del listado | ZIP generado con 69 archivos rastreados existentes; omite los eliminados. No es una entrega final ni incluye archivos nuevos sin rastrear. | Incorporar fuentes nuevas y evidencias al preparar el paquete final. |
| 18/11/2026 | Corrección de advertencias y actualización autorizada de versiones | Textos en recursos, formato numérico con locale explícito, layouts e interruptor corregidos, recursos sin uso retirados. Arranque directo en `MainActivity` sin espera artificial. Gradle 9.6.0 con checksum oficial, AGP 9.5.1, AndroidX/Material actualizados y targetSdk 37; minSdk permanece en 26. | Validar interfaz y arranque físicamente; sincronizar versiones del Word al cerrar el reporte. |
| 18/11/2026 | Validación final: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` | Correcta después de eliminar también dos llamadas a `super.onCleared()` marcadas por Lifecycle nuevo. 14 pruebas unitarias aprobadas, ambos APK generados y lint: `No issues found` (0 errores, 0 advertencias). Sin nuevas supresiones ni baseline de lint. | Pruebas instrumentadas y recorrido manual: ADB no detectó dispositivos conectados. |

| 18/11/2026 | Fases 2 y 3: Room, importación del horario, edición, adquisición compartida, guardado de comparaciones y XML Expressive con View Binding/ListAdapter/ViewModel/LiveData. `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` | Correcto: 27 pruebas unitarias, 0 fallos; APK debug y de pruebas generados; lint sin incidencias. Esquema Room v1 exportado en `app/schemas/`. | Ejecutar pruebas instrumentadas y revisión visual/física cuando haya teléfono. Otros módulos en `docs/PENDIENTES_INTEGRACION.md`. |

| 18/11/2026 | Fases 4 y 5: avisos persistentes, recuperación, historial/borrado, reproducción de comparaciones y respiración. `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` | Correcto: 31 pruebas unitarias, 0 fallos; ambos APK generados y lint sin incidencias. Migración v1→v2 y pruebas instrumentadas de ciclo de vida compiladas. | Ejecutar instrumentadas y validar alarmas, permisos, reinicio y presentación en el teléfono. |

| 18/11/2026 | Fases 6 y 7: directorio/importación, Room v3, navegación inferior, ajustes/restablecimiento, ayuda y logo. `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` | 31 unitarias aprobadas; ambos APK generados; lint sin incidencias. Pruebas instrumentadas de migración, importación y restablecimiento compiladas. | Ejecutar instrumentadas y recorrido físico; comprobar rotación, fuentes grandes, enlaces y borrado. |

El primer intento de Gradle en el entorno restringido no pudo crear su bloqueo
en `C:\.gradle`. La ejecución registrada utilizó la caché del usuario y terminó
correctamente; no se alteró `local.properties` ni la configuración del proyecto.
Las pruebas instrumentadas solo se compilaron; no se ejecutaron en un dispositivo.

Referencias técnicas de esta actualización:
[compatibilidad de AGP 9.5](https://developer.android.com/build/releases/agp-9-5-0-release-notes),
[AndroidX Activity](https://developer.android.com/jetpack/androidx/releases/activity),
[AndroidX AppCompat](https://developer.android.com/jetpack/androidx/releases/appcompat),
[AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test) y
[arranque en Android 12 y posteriores](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate).

## Criterio de cierre

Las funciones acordadas están conectadas y verificadas, pasan las pruebas
relevantes, existen evidencias reales y el reporte describe la aplicación
entregada. No exigir wearable, publicación comercial ni nueva arquitectura para
cumplir este alcance académico.
