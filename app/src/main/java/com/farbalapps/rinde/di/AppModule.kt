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
        
        return Room.databaseBuilder(
            context,
            RindeDatabase::class.java,
            "rinde_database"
        ).addMigrations(MIGRATION_6_7, MIGRATION_18_19)
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
    fun provideFeedRepository(
        paginationDelegate: com.farbalapps.rinde.data.repository.delegate.FeedPaginationDelegate,
        interactionDelegate: com.farbalapps.rinde.data.repository.delegate.FeedInteractionDelegate,
        lifecycleDelegate: com.farbalapps.rinde.data.repository.delegate.FeedLifecycleDelegate
    ): FeedRepository {
        return FeedRepositoryImpl(paginationDelegate, interactionDelegate, lifecycleDelegate)
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
}
