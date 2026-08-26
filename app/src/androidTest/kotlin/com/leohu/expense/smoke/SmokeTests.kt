package com.leohu.expense.smoke

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leohu.expense.BaseTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * L0 冒煙測試 - 驗證基本功能正常運作
 */
@RunWith(AndroidJUnit4::class)
class SmokeTests : BaseTest() {
    
    @Before
    fun setUp() {
        setup()
    }
    
    @Test
    fun testLaunchApp() {
        startMainActivity()
        Thread.sleep(1000)
        // TODO: 實現 Compose UI 測試
    }
    
    @Test
    fun testManualEntry() {
        startMainActivity()
        Thread.sleep(1000)
        // TODO: 實現手動輸入流程
    }
}
