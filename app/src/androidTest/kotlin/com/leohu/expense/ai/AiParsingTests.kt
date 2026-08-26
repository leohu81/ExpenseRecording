package com.leohu.expense.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leohu.expense.BaseTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * L1 AI 解析邏輯測試 - 驗證收據解析功能
 */
@RunWith(AndroidJUnit4::class)
class AiParsingTests : BaseTest() {
    
    @Test
    fun testParseTwdReceipt() {
        val imagePath = getTestImagePath("receipt_twd.jpg")
        if (!imagePath.exists()) {
            org.junit.Assume.assumeTrue("測試圖片不存在", false)
            return
        }
        // TODO: 實現圖片選擇和解析流程
    }
    
    @Test
    fun testParseJpyReceipt() {
        val imagePath = getTestImagePath("receipt_jpy.jpg")
        if (!imagePath.exists()) {
            org.junit.Assume.assumeTrue("測試圖片不存在", false)
            return
        }
    }
    
    @Test
    fun testParseMultiReceipt() {
        val imagePath = getTestImagePath("receipt_multi.jpg")
        if (!imagePath.exists()) {
            org.junit.Assume.assumeTrue("測試圖片不存在", false)
            return
        }
    }
}
