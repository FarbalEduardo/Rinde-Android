package com.farbalapps.rinde.di

import android.content.Context
import androidx.room.Room
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): AuthRepository {
        return FirebaseAuthRepository(firebaseAuth)
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
        
        return Room.databaseBuilder(
            context,
            RindeDatabase::class.java,
            "rinde_database"
        ).addMigrations(MIGRATION_6_7)
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
    fun provideListRepository(
        dao: ShoppingItemDao,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): ListRepository {
        return FirebaseListRepository(dao, firestore, auth)
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
        dao: ProfileDao
    ): ProfileRepository {
        return ProfileRepositoryImpl(dao)
    }
}
