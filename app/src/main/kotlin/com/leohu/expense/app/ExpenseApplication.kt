package com.leohu.expense.app

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.leohu.expense.data.local.db.AppDatabase
import com.leohu.expense.data.remote.api.AgnesApi
import com.leohu.expense.data.remote.api.WebhookApi
import com.leohu.expense.data.remote.db.PostgreSQLClient
import com.leohu.expense.data.remote.dto.AgnesResponseDto
import com.leohu.expense.data.repository.ExpenseRepositoryImpl
import com.leohu.expense.domain.model.*
import com.leohu.expense.domain.repository.ExpenseRepository
import com.leohu.expense.util.PreferenceHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ExpenseApplication : Application() {
    
    lateinit var repository: ExpenseRepository
        private set
    
    lateinit var preferenceHelper: PreferenceHelper
        private set
        
    lateinit var pgClient: PostgreSQLClient
        private set

    // v3 -> v4: 新增 amountTwd 欄位
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                database.execSQL("ALTER TABLE payment_records ADD COLUMN amountTwd REAL")
            } catch (e: Exception) {}
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                database.execSQL("ALTER TABLE source_images ADD COLUMN tags TEXT")
            } catch (e: Exception) {}
        }
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                database.execSQL("CREATE TABLE IF NOT EXISTS tags (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, color TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS payment_record_tags (paymentRecordId TEXT NOT NULL, tagId TEXT NOT NULL, PRIMARY KEY(paymentRecordId, tagId), FOREIGN KEY(paymentRecordId) REFERENCES payment_records(id) ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE)")
                database.execSQL("ALTER TABLE payment_records ADD COLUMN tags TEXT")
                database.execSQL("ALTER TABLE source_images ADD COLUMN preDescription TEXT")
            } catch (e: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        preferenceHelper = PreferenceHelper(applicationContext)

        try {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "expense-db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()

            val retrofitAgnes = Retrofit.Builder()
                .baseUrl("https://apihub.agnes-ai.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            val retrofitWebhook = Retrofit.Builder()
                .baseUrl("http://leohu.ddns.net:5678/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            val agnesApi = retrofitAgnes.create(AgnesApi::class.java)
            val webhookApi = retrofitWebhook.create(WebhookApi::class.java)

            repository = ExpenseRepositoryImpl(applicationContext, db, agnesApi, webhookApi)
            
            // Initialize PostgreSQL client
            pgClient = PostgreSQLClient(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
            repository = createDefaultRepository()
            pgClient = PostgreSQLClient(applicationContext)
        }
    }
    
    private fun createDefaultRepository(): ExpenseRepository {
        return object : ExpenseRepository {
            override suspend fun enqueueSourceImage(localPath: String): String = ""
            override fun getSourceImagesByStatus(status: SourceImageStatus) = flow { emit(emptyList<SourceImage>()) }
            override suspend fun getSourceImageById(id: String): SourceImage? = null
            override fun getAllSourceImages() = flow { emit(emptyList<SourceImage>()) }
            override suspend fun updateSourceImage(image: SourceImage) {}
            override suspend fun deleteSourceImage(id: String) {}
            override fun getPaymentRecordsByStatus(status: PaymentStatus) = flow { emit(emptyList<PaymentRecord>()) }
            override suspend fun getPaymentRecordById(id: String): PaymentRecord? = null
            override fun getPaymentRecordByIdFlow(id: String) = flow { emit(null as PaymentRecord?) }
            override suspend fun createPaymentRecords(imageId: String, transactions: List<PaymentRecord>) {}
            override suspend fun approvePaymentRecord(recordId: String) {}
            override suspend fun updatePaymentRecord(record: PaymentRecord) {}
            override suspend fun deletePaymentRecord(record: PaymentRecord) {}
            override suspend fun deleteOldApprovedRecords(days: Int) {}
            override suspend fun sendToWebhook(record: PaymentRecord) {}
            override fun getCreditCards() = flow { emit(emptyList<CreditCard>()) }
            override suspend fun addCreditCard(card: CreditCard) {}
            override suspend fun deleteCreditCard(card: CreditCard) {}
            override fun getEWalletAccounts() = flow { emit(emptyList<EWalletAccount>()) }
            override suspend fun getAllEWalletAccounts() = emptyList<EWalletAccount>()
            override suspend fun addEWalletAccount(account: EWalletAccount) {}
            override suspend fun deleteEWalletAccount(account: EWalletAccount) {}
            override fun getAllTags() = flow { emit(emptyList<Tag>()) }
            override suspend fun getTagById(id: String): Tag? = null
            override suspend fun addTag(tag: Tag) {}
            override suspend fun deleteTag(tag: Tag) {}
            override suspend fun updateTag(tag: Tag) {}
            override suspend fun getTagsForPaymentRecord(paymentRecordId: String) = flow { emit(emptyList<Tag>()) }
            override suspend fun addTagToPayment(paymentRecordId: String, tagId: String) {}
            override suspend fun removeTagFromPayment(paymentRecordId: String, tagId: String) {}
            override suspend fun parseVoiceInput(
                voiceText: String,
                ewalletAccounts: List<EWalletAccount>,
                creditCards: List<CreditCard>
            ): AgnesResponseDto = throw UnsupportedOperationException("Voice input not supported in default repository")
        }
    }
}
