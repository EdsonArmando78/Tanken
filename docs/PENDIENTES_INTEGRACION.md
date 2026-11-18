# Pendientes de integración

Inicio, Horario y Movimiento usan Java, XML, View Binding y Material 3 Expressive.
Las siguientes piezas se conservan para decidir su destino al cerrar la app:

| Pieza | Acceso actual | Decisión pendiente |
| --- | --- | --- |
| Calculadora y saludo | Otros accesos de Inicio | Retirar o justificar su función en el producto. |
| Tutorial y vídeo de ejemplo | Tutorial desde Inicio | Sustituir por ayuda del producto; revisar recurso de vídeo antes de retirarlo. |
| Ajustes de ejemplo | Ajustes desde Inicio | Sustituir campos decorativos por preferencias y permisos efectivos. |
| Acerca de | Inicio | Revisar contenido y migrar presentación. |
| Temporizadores | Inicio | Conservar como herramienta independiente; migrar interfaz y bindings. |
| Comparación | Inicio | Ya guarda resultados en Room; faltan consulta del historial y renovación visual. Conservar por ahora sus esperas controladas. |

Fases siguientes: motor de avisos/alarmas, historial con filtros y borrado confirmado,
respiración, servicios/importación y navegación final. Las consultas observables de
Room y los estilos compartidos quedan disponibles para esas pantallas.

Validación física pendiente: actualizar sin desinstalar, comprobar migración del
horario, editar/cancelar, rotar con formulario abierto, navegar con lectura activa,
pausar y volver del segundo plano. Revisar también colores dinámicos, tema oscuro,
fuentes grandes y teclado. No se ejecutaron pruebas en un teléfono en esta entrega.
