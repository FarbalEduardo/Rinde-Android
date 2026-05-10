package com.farbalapps.rinde.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.farbalapps.rinde.data.local.dao.CategoryDao
import com.farbalapps.rinde.data.local.dao.ProfileDao
import com.farbalapps.rinde.data.local.dao.ShoppingItemDao
import com.farbalapps.rinde.data.local.entity.CategoryEntity
import com.farbalapps.rinde.data.local.entity.ProfileEntity
import com.farbalapps.rinde.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RindeDatabaseTest {

    private lateinit var db: RindeDatabase
    private lateinit var profileDao: ProfileDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var shoppingItemDao: ShoppingItemDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, RindeDatabase::class.java
        ).build()
        profileDao = db.profileDao()
        categoryDao = db.categoryDao()
        shoppingItemDao = db.shoppingItemDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadProfile() = runBlocking {
        val profile = ProfileEntity(
            id = "test_user",
            name = "Test User",
            email = "test@example.com",
            photoUrl = null,
            followersCount = 10,
            followingCount = 20,
            postsCount = 5,
            rating = 4.5f,
            reviewsCount = 3,
            isPrivate = false,
            isDummy = false
        )
        profileDao.insertProfile(profile)
        val result = profileDao.getProfile("test_user").first()
        assertNotNull(result)
        assertEquals(profile.name, result?.name)
        assertEquals(profile.isPrivate, result?.isPrivate)
        assertEquals(profile.isDummy, result?.isDummy)
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadCategory() = runBlocking {
        val category = CategoryEntity(
            name = "Groceries",
            userId = "test_user",
            isCustom = true,
            orderIndex = 1
        )
        categoryDao.insert(category)
        val categories = categoryDao.getCategoriesByUser("test_user").first()
        assertEquals(1, categories.size)
        assertEquals("Groceries", categories[0].name)
        assertEquals(1, categories[0].orderIndex)
    }

    @Test
    @Throws(Exception::class)
    fun writeAndReadShoppingItem() = runBlocking {
        val item = ShoppingItemEntity(
            id = "item_1",
            name = "Milk",
            category = "Groceries",
            isCompleted = false,
            quantity = 1.0,
            unit = "liter",
            emoji = "🥛",
            listGroup = "Default",
            userId = "test_user"
        )
        shoppingItemDao.insert(item)
        val items = shoppingItemDao.getItemsByUser("test_user").first()
        assertEquals(1, items.size)
        assertEquals("Milk", items[0].name)
        assertEquals("Default", items[0].listGroup)
    }
}
