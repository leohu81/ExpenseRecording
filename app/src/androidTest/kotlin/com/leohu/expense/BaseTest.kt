package com.leohu.expense

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import java.io.File

/**
 * 基礎測試類別，提供 Activity 啟動和測試資源訪問
 */
open class BaseTest {
    
    protected lateinit var scenario: ActivityScenario<com.leohu.expense.app.MainActivity>
    protected lateinit var context: Context
    
    /**
     * 初始化測試環境
     */
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }
    
    /**
     * 啟動 Activity
     */
    fun startMainActivity() {
        scenario = ActivityScenario.launch(com.leohu.expense.app.MainActivity::class.java)
    }
    
    /**
     * 獲取測試圖片路徑
     */
    fun getTestImagePath(name: String): File {
        return File("/data/local/tmp/test_resources/$name")
    }
    
    /**
     * 獲取備份文件路徑
     */
    fun getBackupFilePath(name: String): File {
        return File("/data/local/tmp/test_resources/$name")
    }
}
