package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.SyncMetadataDao
import com.farbalapps.rinde.data.local.entity.SyncMetadataEntity
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.data.mapper.toDomain
import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.SearchResult
import com.farbalapps.rinde.domain.repository.SearchRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val postDao: PostDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val firestore: FirebaseFirestore
) : SearchRepository {

    companion object {
        private const val LOCAL_RESULT_THRESHOLD = 5
        private const val SEARCH_CACHE_TTL_MS = 5 * 60 * 1000L // 5 min TTL por query
        private const val FIRESTORE_LIMIT = 20L
    }

    override fun searchPosts(query: String, categoryFilter: String): Flow<SearchResult> = flow {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) {
            emit(SearchResult.Idle)
            return@flow
        }

        emit(SearchResult.Loading)

        // 1. Búsqueda local FTS4 / LIKE en Room
        val ftsFormattedQuery = "${cleanQuery.replace("\"", "").replace("'", "")}*"
        var localList = try {
            postDao.searchPostsFts(ftsFormattedQuery, 50)
        } catch (e: Exception) {
            emptyList()
        }

        // Fallback a LIKE si FTS4 no devolvió nada o tuvo un syntax error
        if (localList.isEmpty()) {
            try {
                localList = postDao.searchPostsLike(cleanQuery, 50)
            } catch (e: Exception) {
                // Ignore fallback error
            }
        }

        var domainPosts = localList.map { it.toDomainModel() }
        if (categoryFilter.isNotBlank()) {
            domainPosts = domainPosts.filter { post ->
                val matchesCategory = post.category.equals(categoryFilter, ignoreCase = true)
                val matchesOfferType = when (categoryFilter.uppercase()) {
                    "ONLINE", "EN LÍNEA", "EN LINEA" -> post.offerType == OfferType.ONLINE
                    "PHYSICAL", "FÍSICO", "FISICO", "FÍSICA", "FISICA" -> post.offerType == OfferType.PHYSICAL
                    else -> false
                }
                matchesCategory || matchesOfferType
            }
        }

        // Emitir primera versión de resultados (Locales)
        emit(SearchResult.Success(domainPosts))

        // 2. Evaluar si es necesario consultar a Firestore
        val shouldFetchRemote = domainPosts.size < LOCAL_RESULT_THRESHOLD && shouldFetchFromFirestore(cleanQuery)
        if (!shouldFetchRemote) {
            return@flow
        }

        // 3. Consulta Firestore acotada
        try {
            val remoteDtos = mutableListOf<CommunityPostDto>()

            // Query por categoría si el query coincide
            val byCategory = firestore.collection("posts")
                .whereEqualTo("isActive", true)
                .whereEqualTo("category", cleanQuery)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(FIRESTORE_LIMIT)
                .get()
                .await()

            remoteDtos.addAll(byCategory.documents.mapNotNull { doc ->
                doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)
            })

            val capitalizedQuery = cleanQuery.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            // Query por tienda (prefijo) si no hay suficientes
            if (remoteDtos.size < LOCAL_RESULT_THRESHOLD) {
                val byStore = firestore.collection("posts")
                    .whereEqualTo("isActive", true)
                    .orderBy("storeName")
                    .whereGreaterThanOrEqualTo("storeName", capitalizedQuery)
                    .whereLessThanOrEqualTo("storeName", capitalizedQuery + "\uf8ff")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(FIRESTORE_LIMIT)
                    .get()
                    .await()

                remoteDtos.addAll(byStore.documents.mapNotNull { doc ->
                    doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)
                })
            }

            // Query por título (prefijo) si no hay suficientes
            if (remoteDtos.size < LOCAL_RESULT_THRESHOLD) {
                val byTitle = firestore.collection("posts")
                    .whereEqualTo("isActive", true)
                    .orderBy("title")
                    .whereGreaterThanOrEqualTo("title", capitalizedQuery)
                    .whereLessThanOrEqualTo("title", capitalizedQuery + "\uf8ff")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(FIRESTORE_LIMIT)
                    .get()
                    .await()

                remoteDtos.addAll(byTitle.documents.mapNotNull { doc ->
                    doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)
                })
            }

            // Eliminar duplicados si los hubiera
            val distinctRemoteDtos = remoteDtos.distinctBy { it.id }

            // 4. Guardar resultados remotos en Room y registrar TTL
            val remoteDomainPosts = distinctRemoteDtos.mapNotNull { it.toDomain() }
            if (remoteDomainPosts.isNotEmpty()) {
                postDao.upsertPosts(remoteDomainPosts.map { it.toEntity() })
            }
            saveSearchTtl(cleanQuery)

            // 5. Volver a consultar Room para emitir la lista unificada
            var updatedLocalList = emptyList<com.farbalapps.rinde.data.local.entity.CommunityPostEntity>()
            try {
                updatedLocalList = postDao.searchPostsLike(cleanQuery, 50)
            } catch (_: Exception) {}

            var updatedDomain = updatedLocalList.map { it.toDomainModel() }
            if (categoryFilter.isNotBlank()) {
                updatedDomain = updatedDomain.filter { post ->
                    val matchesCategory = post.category.equals(categoryFilter, ignoreCase = true)
                    val matchesOfferType = when (categoryFilter.uppercase()) {
                        "ONLINE", "EN LÍNEA", "EN LINEA" -> post.offerType == OfferType.ONLINE
                        "PHYSICAL", "FÍSICO", "FISICO", "FÍSICA", "FISICA" -> post.offerType == OfferType.PHYSICAL
                        else -> false
                    }
                    matchesCategory || matchesOfferType
                }
            }

            emit(SearchResult.Success(updatedDomain))

        } catch (e: Exception) {
            // Si Firestore falla (offline o error cuota), conservamos lo local sin romper UI
            android.util.Log.e("SearchRepositoryImpl", "Error querying remote Firestore: ${e.message}")
        }
    }

    private suspend fun shouldFetchFromFirestore(query: String): Boolean {
        val ttlKey = "search_${query.lowercase().hashCode()}"
        val meta = syncMetadataDao.getMetadata(ttlKey) ?: return true
        return (System.currentTimeMillis() - meta.lastSyncTimestamp) > SEARCH_CACHE_TTL_MS
    }

    private suspend fun saveSearchTtl(query: String) {
        val ttlKey = "search_${query.lowercase().hashCode()}"
        val now = System.currentTimeMillis()
        syncMetadataDao.upsert(
            SyncMetadataEntity(
                key = ttlKey,
                lastSyncTimestamp = now,
                lastSeenTimestamp = now
            )
        )
    }
}
