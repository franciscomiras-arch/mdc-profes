# MdC Profes 0.2 — Accesos rápidos del profesorado (Android 8.0 o superior)

## Qué hace
- **22 accesos** de la Zona Profesorado de la web, agrupados por secciones.
- **Mi horario**: cada profesor añade una foto de su horario (cámara o galería) y la ve a pantalla completa con zoom. No pide permisos y la foto se guarda solo en su móvil.
- **Enlaces propios**: botón «+ Añadir enlace» con nombre, dirección, icono y sección (existente o nueva). Se pueden editar y eliminar.
- **Guardar desde otras apps**: en Chrome, Drive o WhatsApp, «Compartir › MdC Profes» añade la página.
- **Favoritos**, **buscador** sin distinguir acentos, **ocultar** accesos que no uses y **restaurarlos**.
- **Accesos directos** en la pantalla de inicio para cualquier enlace. Al mantener pulsado el icono de la app aparecen «Mi horario» y «Añadir enlace».
- **Compartir e importar** listas de enlaces entre compañeros, por ejemplo de un departamento, como texto: `icono | nombre | dirección | sección`.
- Los enlaces se abren en la **app nativa** si existe (Drive, Gmail…). Si no, en una pestaña del navegador **precargada**, que es más rápida y mantiene las sesiones iniciadas.
- **Modo oscuro**, diseño de borde a borde (Android 15/16), columnas que se adaptan a tabletas y lectura por TalkBack.

## Cómo obtener el APK

### Opción A — GitHub Actions (sin instalar nada)
1. Crea un repositorio **privado** en GitHub y sube esta carpeta.
2. En *Settings › Secrets and variables › Actions*, crea los secretos indicados en `LEEME-FIRMA.txt`. Este paso es opcional, pero muy recomendable.
3. Ve a *Actions › Compilar APK › Run workflow*. En unos 5 minutos podrás descargar el artefacto **MdCProfes-APK**.

### Opción B — Android Studio
1. Abre la carpeta y espera a que termine la sincronización.
2. Ve a *Build › Generate Signed App Bundle or APK › APK* y usa `release.jks`, alias `mdc`.

## Instalación en los móviles
- Envía el APK por correo, Drive o Telegram y ábrelo. Android pedirá permitir «instalar apps desconocidas» desde esa aplicación.
- Play Protect puede avisar de que es una app desconocida: es normal en apps que no vienen de Play Store. Pulsa «Más detalles › Instalar de todos modos».
- **Actualizaciones**: firma siempre con la misma clave `release.jks`. Así se instalan encima de la versión anterior sin perder enlaces ni horario.

## Mantenimiento
- Los accesos del centro están en `app/src/main/java/es/iesmdc/profes/Defaults.java`. No cambies los identificadores (`d_...`): sirven para recordar favoritos y ocultos.
- Antes de cada nueva versión, sube `versionCode` en `app/build.gradle`.
