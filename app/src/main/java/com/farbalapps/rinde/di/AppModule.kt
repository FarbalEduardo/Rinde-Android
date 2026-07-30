package com.farbalapps.rinde.di

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import com.farbalapps.rinde.data.local.dao.CustomProductHistoryDao
import com.farbalapps.rinde.data.local.dao.ShoppingItemDao
import com.farbalapps.rinde.data.local.db.RindeDatabase
import com.farbalapps.rinde.data.repository.FirebaseAuthRepository
import com.farbalapps.rinde.data.repository.FirebaseListRepository
import com.farbalapps.rinde.data.repository.FirebaseUserRepository
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.CustomProductHistoryRepository
import com.farbalapps.rinde.data.repository.CustomProductHistoryRepositoryImpl
import com.farbalapps.rinde.domain.repository.ListRepository
import com.farbalapps.rinde.domain.repository.UserRepository
import com.farbalapps.rinde.data.local.dao.CategoryDao
import com.farbalapps.rinde.domain.repository.CategoryRepository
import com.farbalapps.rinde.data.repository.CategoryRepositoryImpl
import com.farbalapps.rinde.data.local.dao.ProfileDao
import com.farbalapps.rinde.domain.repository.ProfileRepository
import com.farbalapps.rinde.data.repository.ProfileRepositoryImpl
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.data.repository.FeedRepositoryImpl
import com.farbalapps.rinde.domain.repository.CommentRepository
import com.farbalapps.rinde.data.repository.CommentRepositoryImpl
import com.farbalapps.rinde.domain.moderation.ContentModerator
import com.farbalapps.rinde.util.LocationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.database.FirebaseDatabase
import androidx.work.WorkManager
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        savedPostsMemoryCache: com.farbalapps.rinde.data.util.SavedPostsMemoryCache,
        userVoteDao: com.farbalapps.rinde.data.local.dao.UserVoteDao,
        postDao: com.farbalapps.rinde.data.local.dao.PostDao,
        syncMetadataDao: com.farbalapps.rinde.data.local.dao.SyncMetadataDao
    ): AuthRepository {
        return FirebaseAuthRepository(
            firebaseAuth,
            savedPostsMemoryCache,
            userVoteDao,
            postDao,
            syncMetadataDao
        )
    }

    @Provides
    @Singleton
    fun provideUserRepository(firestore: FirebaseFirestore): UserRepository {
        return FirebaseUserRepository(firestore)
    }

    // --- Room ---

    @Provides
    @Singleton
    fun provideRindeDatabase(@ApplicationContext context: Context): RindeDatabase {
        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_votes` (" +
                    "`postId` TEXT NOT NULL, " +
                    "`userId` TEXT NOT NULL, " +
                    "`voteValue` INTEGER NOT NULL, " +
                    "`confirmedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`postId`, `userId`))"
                )
            }
        }

        // MIGRACIÓN 19→20: Agrega las tablas de Metas de Ahorro (SavingsGoalEntity, GoalTransactionEntity).
        // SIN esta migración, Room destruye toda la DB (incluyendo user_votes) al actualizar,
        // causando el error "No se pudo registrar tu voto" al intentar votar.
        val MIGRATION_19_20 = object : androidx.room.migration.Migration(19, 20) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Tabla principal de metas de ahorro
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `savings_goals` (" +
                    "`id` TEXT NOT NULL, " +
                    "`userId` TEXT NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`targetAmount` REAL NOT NULL, " +
                    "`currentAmount` REAL NOT NULL, " +
                    "`iconKey` TEXT NOT NULL, " +
                    "`colorKey` TEXT NOT NULL, " +
                    "`isCompleted` INTEGER NOT NULL DEFAULT 0, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "`monthlySnapshotAmount` REAL NOT NULL DEFAULT 0.0, " +
                    "`isSynced` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`id`))"
                )
                // Tabla de historial de transacciones/depósitos por meta
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goal_transactions` (" +
                    "`id` TEXT NOT NULL, " +
                    "`goalId` TEXT NOT NULL, " +
                    "`amount` REAL NOT NULL, " +
                    "`note` TEXT NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`isSynced` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`goalId`) REFERENCES `savings_goals`(`id`) ON DELETE CASCADE)"
                )
                // Índice requerido por la anotación @Index en GoalTransactionEntity
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_transactions_goalId` ON `goal_transactions` (`goalId`)")
            }
        }

        val MIGRATION_21_22 = object : androidx.room.migration.Migration(21, 22) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `community_posts_fts` USING fts4(content=`community_posts`, title, descriptionShort, storeName, category, authorName, couponCode)")
            }
        }

        val MIGRATION_22_23 = object : androidx.room.migration.Migration(22, 23) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN commentsCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_23_24 = object : androidx.room.migration.Migration(23, 24) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN price REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN currency TEXT NOT NULL DEFAULT 'MXN'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `saved_lists` (" +
                    "`id` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`savedAt` INTEGER NOT NULL, " +
                    "`lastModifiedAt` INTEGER NOT NULL, " +
                    "`totalItems` INTEGER NOT NULL, " +
                    "`completedItems` INTEGER NOT NULL, " +
                    "`totalPrice` REAL, " +
                    "`currency` TEXT NOT NULL DEFAULT 'MXN', " +
                    "`items` TEXT NOT NULL, " +
                    "`userId` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))"
                )
            }
        }
        
        return Room.databaseBuilder(
            context,
            RindeDatabase::class.java,
            "rinde_database"
        ).addMigrations(MIGRATION_6_7, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24)
         .fallbackToDestructiveMigration()
         .build()
     }


    @Provides
    @Singleton
    fun provideShoppingItemDao(db: RindeDatabase): ShoppingItemDao {
        return db.shoppingItemDao()
    }

    @Provides
    @Singleton
    fun provideSavedListDao(db: RindeDatabase): com.farbalapps.rinde.data.local.dao.SavedListDao {
        return db.savedListDao()
    }

    @Provides
    @Singleton
    fun provideCustomProductHistoryDao(db: RindeDatabase): CustomProductHistoryDao {
        return db.customProductHistoryDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(db: RindeDatabase): CategoryDao {
        return db.categoryDao()
    }

    @Provides
    @Singleton
    fun provideUserVoteDao(db: RindeDatabase): com.farbalapps.rinde.data.local.dao.UserVoteDao {
        return db.userVoteDao()
    }




    @Provides
    @Singleton
    fun provideListRepository(
        dao: ShoppingItemDao,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        workManager: WorkManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ListRepository {
        return FirebaseListRepository(dao, firestore, auth, workManager, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideSavedListRepository(
        dao: com.farbalapps.rinde.data.local.dao.SavedListDao,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        workManager: WorkManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): com.farbalapps.rinde.domain.repository.SavedListRepository {
        return com.farbalapps.rinde.data.repository.FirebaseSavedListRepository(dao, firestore, auth, workManager, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideCustomProductHistoryRepository(
        dao: CustomProductHistoryDao
    ): CustomProductHistoryRepository {
        return CustomProductHistoryRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(
        dao: CategoryDao,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): CategoryRepository {
        return CategoryRepositoryImpl(dao, firestore, auth)
    }

    @Provides
    @Singleton
    fun provideProfileDao(db: RindeDatabase): ProfileDao {
        return db.profileDao()
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        crudDelegate: com.farbalapps.rinde.data.repository.delegate.ProfileCrudDelegate,
        privacyDelegate: com.farbalapps.rinde.data.repository.delegate.ProfilePrivacyDelegate,
        socialDelegate: com.farbalapps.rinde.data.repository.delegate.ProfileSocialDelegate
    ): ProfileRepository {
        return ProfileRepositoryImpl(crudDelegate, privacyDelegate, socialDelegate)
    }

    @Provides
    @Singleton
    fun providePostDao(db: RindeDatabase): com.farbalapps.rinde.data.local.dao.PostDao {
        return db.postDao()
    }

    @Provides
    @Singleton
    fun provideSyncMetadataDao(db: RindeDatabase): com.farbalapps.rinde.data.local.dao.SyncMetadataDao {
        return db.syncMetadataDao()
    }

    @Provides
    @Singleton
    fun providePendingVoteDao(db: RindeDatabase): com.farbalapps.rinde.data.local.dao.PendingVoteDao {
        return db.pendingVoteDao()
    }

    @Provides
    @Singleton
    fun provideFeedRepository(
        paginationDelegate: com.farbalapps.rinde.data.repository.delegate.FeedPaginationDelegate,
        interactionDelegate: com.farbalapps.rinde.data.repository.delegate.FeedInteractionDelegate,
        lifecycleDelegate: com.farbalapps.rinde.data.repository.delegate.FeedLifecycleDelegate
    ): FeedRepository {
        return FeedRepositoryImpl(paginationDelegate, interactionDelegate, lifecycleDelegate)
    }

    @Provides
    @Singleton
    fun provideSearchRepository(
        postDao: com.farbalapps.rinde.data.local.dao.PostDao,
        syncMetadataDao: com.farbalapps.rinde.data.local.dao.SyncMetadataDao,
        firestore: FirebaseFirestore
    ): com.farbalapps.rinde.domain.repository.SearchRepository {
        return com.farbalapps.rinde.data.repository.SearchRepositoryImpl(postDao, syncMetadataDao, firestore)
    }

    @Provides
    @Singleton
    fun provideSearchHistoryDataStore(
        @ApplicationContext context: Context
    ): com.farbalapps.rinde.data.local.SearchHistoryDataStore {
        return com.farbalapps.rinde.data.local.SearchHistoryDataStore(context)
    }


    @Provides
    @Singleton
    fun provideCommentRepository(
        @ApplicationContext context: Context,
        database: FirebaseDatabase,
        firestore: FirebaseFirestore
    ): CommentRepository {
        return CommentRepositoryImpl(context, database, firestore)
    }

    @Provides
    @Singleton
    fun provideContentModerator(): ContentModerator {
        return ContentModerator()
    }

    @Provides
    @Singleton
    fun provideLocationService(@ApplicationContext context: Context): LocationService {
        return LocationService(context)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsManager: com.farbalapps.rinde.data.local.SettingsManager,
        profileRepository: ProfileRepository,
        sessionManager: com.farbalapps.rinde.data.local.SessionManager
    ): com.farbalapps.rinde.domain.repository.SettingsRepository {
        return com.farbalapps.rinde.data.repository.SettingsRepositoryImpl(
            settingsManager,
            profileRepository,
            sessionManager
        )
    }

    @Provides
    @Singleton
    fun provideGoalsDao(db: RindeDatabase): com.farbalapps.rinde.data.local.dao.GoalsDao {
        return db.goalsDao()
    }

    @Provides
    @Singleton
    fun provideGoalsRepository(
        dao: com.farbalapps.rinde.data.local.dao.GoalsDao,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        workManager: WorkManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): com.farbalapps.rinde.domain.repository.GoalsRepository {
        return com.farbalapps.rinde.data.repository.FirebaseGoalsRepository(
            dao, firestore, auth, workManager, ioDispatcher
        )
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        firestore: FirebaseFirestore,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): com.farbalapps.rinde.domain.repository.NotificationRepository {
        return com.farbalapps.rinde.data.repository.NotificationRepositoryImpl(firestore, ioDispatcher)
    }
}

