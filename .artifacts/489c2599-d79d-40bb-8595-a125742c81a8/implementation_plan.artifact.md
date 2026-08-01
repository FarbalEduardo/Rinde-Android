# Solución al Problema de Búsqueda en Comunidad

El usuario reporta que la búsqueda no arroja resultados (se queda en "skeletons") incluso buscando productos existentes por su título.

## Análisis del Problema

1.  **Bloqueo del Flujo (Causa Principal de Skeletons)**: En `SearchRepositoryImpl.kt`, se están llamando a `.collect` sobre flujos de Room (`postDao.searchPostsFts` y `postDao.searchPostsLike`) dentro de un bloque `flow { ... }`. Los flujos de Room son infinitos (no terminan), por lo que `.collect` suspende la ejecución indefinidamente. Esto evita que el repositorio emita los resultados (`SearchResult.Success`) y la UI se queda atrapada en el estado `Loading` (skeletons).
2.  **Búsqueda Remota Limitada**: La implementación actual de Firestore en `SearchRepositoryImpl` solo busca por `category` y `storeName` de forma exacta. Si el usuario busca por el título del producto, Firestore no devuelve nada si no hay coincidencia exacta en esos otros campos.

## Cambios Propuestos

### [Componente de Datos]

#### [MODIFY] [SearchRepositoryImpl.kt](file:///D:/PROYECTOSANDROID/Rinde/app/src/main/java/com/farbalapps/rinde/data/repository/SearchRepositoryImpl.kt)

- Reemplazar el uso de `.collect` por `.first()` (o `.firstOrNull()`) para obtener la captura actual de los datos locales de Room sin suscribirse a un flujo infinito.
- Ampliar las consultas de Firestore para incluir la búsqueda por `title`.
- Implementar una lógica de búsqueda remota más robusta:
    - Búsqueda exacta por `title`.
    - Búsqueda por prefijo en `title` (usando `whereGreaterThanOrEqualTo` y `whereLessThanOrEqualTo`).
    - Mantener las búsquedas por `category` y `storeName`.
- Asegurar que los resultados remotos se unifiquen sin duplicados antes de guardarlos en Room.
- Corregir la consulta final a Room después de la descarga remota para que también use `.first()` y no bloquee.

## Plan de Verificación

### Pruebas Automatizadas
- No se requieren nuevas pruebas unitarias en esta etapa, pero se verificará que no se rompan las existentes si las hubiera.

### Verificación Manual
- Realizar una búsqueda en la app con un título de producto conocido.
- Verificar que los resultados aparezcan rápidamente (primero locales, luego remotos si aplica).
- Confirmar que el estado de "skeleton" desaparece una vez obtenidos los resultados.
- Probar filtros de categoría para asegurar que sigan funcionando.
