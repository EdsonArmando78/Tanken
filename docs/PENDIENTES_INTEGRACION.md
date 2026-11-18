# Pendientes de integración

Las pantallas principales y las nuevas usan Java, XML, View Binding y Material 3 Expressive.
Las siguientes piezas se conservan para decidir su destino al cerrar la app:

| Pieza | Acceso actual | Decisión pendiente |
| --- | --- | --- |
| Calculadora y saludo | Otros accesos de Inicio | Retirar o justificar su función en el producto. |
| Tutorial y vídeo de ejemplo | Tutorial desde Inicio | Sustituir por ayuda del producto; revisar recurso de vídeo antes de retirarlo. |
| Ajustes de ejemplo | Ajustes desde Inicio | Sustituir campos decorativos por preferencias y permisos efectivos. |
| Acerca de | Inicio | Revisar contenido y migrar presentación. |
| Temporizadores | Inicio | Conservar como herramienta independiente; migrar interfaz y bindings. |
| Comparación | Inicio e Historial | Conservar el experimento como herramienta opcional; ya permite consultar y reproducir resultados sin esperas artificiales. |

Fases siguientes: servicios/importación, ajustes y navegación final. Recordatorios,
historial y respiración están implementados; falta su validación física.

Validación física pendiente: actualizar sin desinstalar, comprobar migración del
horario, editar/cancelar, rotar con formulario abierto, navegar con lectura activa,
pausar y volver del segundo plano. Revisar también colores dinámicos, tema oscuro,
fuentes grandes y teclado. En fases 4 y 5 comprobar avisos con la app cerrada,
permiso/canal bloqueado, reinicio, acciones repetidas, borrado, reproducción y pausa
de respiración. Las pruebas instrumentadas solo se compilaron; no se ejecutaron.
