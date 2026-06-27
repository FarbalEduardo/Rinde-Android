---
name: firestore-denormalization-skill
description: Guía de mejores prácticas de desnormalización en Firestore, consistencia eventual, Smart Interest Feeds y arquitectura escalable NoSQL.
---

# 🗄️ Skill: Firestore Denormalization & NoSQL Architecture

Esta skill documenta y prescribe los estándares técnicos para la gestión de redundancia de datos, duplicación estratégica de datos (desnormalización) y consistencia eventual en la base de datos Firestore del proyecto "Rinde".

---

## 🏗️ Principios Fundamentales NoSQL
1. **Optimizar para Lecturas**: Las consultas en bases de datos NoSQL deben ser rápidas y requerir la menor cantidad de queries secundarias (N+1 queries).
2. **Duplicación Estratégica**: Duplicar datos está bien si reduce de manera dramática las lecturas necesarias para renderizar una pantalla.
3. **Consistencia Eventual**: Aceptar que cuando un dato principal cambia, sus réplicas pueden tardar unos milisegundos en actualizarse mediante operaciones por lotes (Batch-Writes) o procesos asíncronos.

---

## 💡 Patrones de Desnormalización en Rinde

### 1. Author Snapshot Propagation
* **Objetivo**: Asegurar que cuando el autor actualice su perfil (nombre o foto), todos sus posts históricos en la colección `/posts` muestren la información actualizada.
* **Implementación**:
  * Al actualizar el perfil en `ProfileRepository.updateProfile()`, realizar una consulta de los posts del usuario.
  * Ejecutar un Batch-Write en Firestore para actualizar los campos `authorName` y `authorPhotoUrl` en todos los posts de ese autor.
  * *Límite*: Las operaciones Batch tienen un límite de 500 escrituras. Si el usuario tiene más de 500 posts, esta lógica debe ser relegada a una Cloud Function o procesarse en batches secuenciales.

### 2. Post Snapshot en `saved_posts`
* **Objetivo**: Evitar lecturas N+1 y el límite duro de 30 posts por query (`whereIn`) en la pantalla de posts guardados.
* **Implementación**:
  * La subcolección `/users/{userId}/saved_posts/{postId}` no debe guardar únicamente la fecha de guardado (`savedAt`).
  * Debe almacenar una réplica desnormalizada de los campos clave del post (`postSnapshot`), de modo que la pantalla de favoritos se renderice en una sola lectura directa de la subcolección.
  * El snapshot debe incluir: `title`, `category`, `storeName`, `authorName`, `authorPhotoUrl`, `photos`, etc.

### 3. Smart Interest Feed (Feed por Intereses Declarados)
* **Objetivo**: Mostrar posts altamente relevantes para el usuario basándose en sus intereses declarados de forma explícita, evitando las limitaciones de escalabilidad de un feed basado puramente en follows gestionado en el cliente.
* **Estructura**:
  * `/users/{userId}` expone los arreglos `interests` (categorías preferidas) y `zonasDeCaza` (zonas geográficas).
  * La query del feed recupera directamente los posts que coincidan con la categoría del interés del usuario usando la indexación nativa de Firestore.

---

## 🔮 Evolución Futura: Fan-Out Feed (Cloud Functions)
Cuando la escala de usuarios crezca significativamente y se requiera un feed basado puramente en las personas a las que sigues, se debe adoptar el patrón **Fan-Out on Write**:

1. **Creación**: Un usuario publica un post.
2. **Trigger**: Una Cloud Function se activa tras la creación del documento en `/posts/{postId}`.
3. **Distribución**: La función consulta los seguidores del autor en `/relationships` y escribe una réplica del post (o de su referencia con snapshot) en la subcolección `/user_feeds/{followerId}/feed/{postId}` de cada seguidor.
4. **Ventaja**: El seguidor obtiene su feed personalizado haciendo una sola query ordenada cronológicamente a su propia subcolección `/user_feeds/{userId}/feed`, escalando para millones de usuarios.
