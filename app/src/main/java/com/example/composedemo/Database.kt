package com.example.composedemo

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val password: String
)

@Entity(
    tableName = "spend_records",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["email"],
            childColumns = ["userEmail"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class SpendRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val amount: Double,
    val category: String,
    val description: String,
    val date: LocalDate,
    val cardId: Int? = null
)

@Entity(
    tableName = "category_limits",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["email"],
            childColumns = ["userEmail"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CategoryLimitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val category: String,
    val limitAmount: Double
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["email"],
            childColumns = ["userEmail"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val cardNumber: String,
    val cardHolderName: String,
    val expiryDate: String,
    val cvv: String,
    val cardType: String // e.g., "Visa", "Mastercard"
)

@Dao
interface SpendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SpendRecordEntity)

    @Delete
    suspend fun deleteRecord(record: SpendRecordEntity)

    @Query("SELECT * FROM spend_records WHERE userEmail = :email ORDER BY date DESC")
    fun getRecordsForUser(email: String): Flow<List<SpendRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUser(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLimit(limit: CategoryLimitEntity)

    @Query("SELECT * FROM category_limits WHERE userEmail = :email")
    fun getLimitsForUser(email: String): Flow<List<CategoryLimitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("SELECT * FROM cards WHERE userEmail = :email")
    fun getCardsForUser(email: String): Flow<List<CardEntity>>
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? {
        return date?.toString()
    }
}

@Database(entities = [UserEntity::class, SpendRecordEntity::class, CategoryLimitEntity::class, CardEntity::class], version = 4)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spendDao(): SpendDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spend_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
