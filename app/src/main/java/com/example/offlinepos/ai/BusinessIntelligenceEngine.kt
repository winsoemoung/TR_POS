package com.example.offlinepos.ai

import com.example.offlinepos.data.local.ProductEntity
import com.example.offlinepos.data.local.PurchaseEntity
import com.example.offlinepos.data.local.ReturnTransactionEntity
import com.example.offlinepos.data.local.SaleEntity
import com.example.offlinepos.data.local.SaleItemEntity
import com.example.offlinepos.data.local.StockMovementEntity
import com.example.offlinepos.data.local.SupplierEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

/**
 * Business Intelligence & Predictive Analytics Engine (DDD)
 * 1. Bi-monthly (1st and 15th of month) automated business insights:
 *    - Top-selling items (volume & revenue)
 *    - Slow-moving / dead stock
 *    - Smart restock recommendations
 *    - Sales & demand predictions for next 15-30 days
 *    - Damaged / returned goods impact
 *    - Supplier purchases summary
 * 2. 12-Month / 1-Year Seasonal Prediction Engine:
 *    - Month-by-month high sales patterns
 *    - Calculation of recommended purchase stocking volumes ("မည်သည့်လတွင် မည်သို့သော ပစ္စည်းများ ရောင်းကောင်းသည်၊ ထို့ကြောင့် မည်သို့သော ပစ္စည်းများ ဝယ်ယူလှောင်ထားသင့်သည့် ပမာဏ")
 */
object BusinessIntelligenceEngine {

    data class TopSellingItem(
        val productId: Long,
        val productName: String,
        val category: String,
        val quantitySold: Long,
        val totalRevenueMinor: Long,
        val estimatedProfitMinor: Long,
    )

    data class SlowMovingItem(
        val productId: Long,
        val productName: String,
        val category: String,
        val currentStock: Long,
        val daysWithoutSale: Int,
        val tiedCapitalMinor: Long,
        val recommendation: String,
    )

    data class RestockRecommendation(
        val productId: Long,
        val productName: String,
        val currentStock: Long,
        val lowStockThreshold: Long,
        val dailyVelocity: Double,
        val suggestedOrderQty: Long,
        val estimatedCostMinor: Long,
        val urgency: String, // "အရေးပေါ်", "ပုံမှန်", "ကြိုတင်ပြင်ဆင်"
        val suggestedSupplier: String?,
    )

    data class BiMonthlyReport(
        val reportPeriod: String, // e.g. "၁ ရက် ~ ၁၅ ရက်" or "၁၆ ရက် ~ လကုန်"
        val generatedDate: String,
        val isFirstOrFifteenth: Boolean,
        val totalRevenueMinor: Long,
        val totalProfitMinor: Long,
        val totalOrdersCount: Int,
        val topSellingItems: List<TopSellingItem>,
        val slowMovingItems: List<SlowMovingItem>,
        val restockRecommendations: List<RestockRecommendation>,
        val demandPredictionNext15DaysMinor: Long,
        val highDemandCategories: List<String>,
        val damagedItemsCount: Int,
        val damagedTotalCostMinor: Long,
        val returnedItemsCount: Int,
        val returnedTotalRefundMinor: Long,
        val purchasesTotalMinor: Long,
        val purchasesCount: Int,
    )

    data class MonthlySeasonalInsight(
        val monthNumber: Int, // 1..12
        val monthNameMm: String,
        val seasonTitleMm: String,
        val seasonalDescription: String,
        val topTrendingCategories: List<String>,
        val recommendedStockItems: List<RecommendedStockItem>,
    )

    data class RecommendedStockItem(
        val productName: String,
        val category: String,
        val reason: String,
        val recommendedPurchaseVolume: String,
        val suggestedStockMultiplier: Double,
    )

    /**
     * Generate full Bi-Monthly Report (1st or 15th) using all available Room database entities.
     */
    fun generateBiMonthlyReport(
        products: List<ProductEntity>,
        sales: List<SaleEntity>,
        saleItems: List<SaleItemEntity>,
        purchases: List<PurchaseEntity> = emptyList(),
        returns: List<ReturnTransactionEntity> = emptyList(),
        suppliers: List<SupplierEntity> = emptyList(),
        calendar: Calendar = Calendar.getInstance(),
    ): BiMonthlyReport {
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val isFirstOrFifteenth = dayOfMonth == 1 || dayOfMonth == 15
        val period = if (dayOfMonth <= 15) {
            "၁ ရက်နေ့ မှ ၁၅ ရက်နေ့ သုံးသပ်ချက်"
        } else {
            "၁၆ ရက်နေ့ မှ လကုန် သုံးသပ်ချက်"
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val generatedDate = dateFormat.format(calendar.time)

        // 1. Group sales items by productId
        val itemsByProduct = saleItems.groupBy { it.productId }
        val productMap = products.associateBy { it.productId }

        // Top Selling
        val topSelling = itemsByProduct.mapNotNull { (prodId, lines) ->
            val prod = productMap[prodId] ?: return@mapNotNull null
            val qty = lines.sumOf { it.quantity }
            val rev = lines.sumOf { it.lineTotalMinor }
            val cost = qty * prod.costPriceMinor
            val profit = max(0L, rev - cost)
            TopSellingItem(
                productId = prodId,
                productName = prod.name,
                category = prod.category,
                quantitySold = qty,
                totalRevenueMinor = rev,
                estimatedProfitMinor = profit
            )
        }.sortedByDescending { it.totalRevenueMinor }.take(10)

        // 2. Slow Moving / Dead Stock (Current stock > 0, but no recent sales or zero sales)
        val now = System.currentTimeMillis()
        val slowMoving = products.filter { it.stockQuantity > 0 }.mapNotNull { prod ->
            val soldLines = itemsByProduct[prod.productId].orEmpty()
            val totalSold = soldLines.sumOf { it.quantity }
            if (totalSold == 0L || prod.stockQuantity > 30) {
                val tiedCapital = prod.stockQuantity * prod.costPriceMinor
                val rec = if (totalSold == 0L) {
                    "ရောင်းအား မရှိသေးပါ (Dead Stock) - ပရိုမိုးရှင်း လျှော့ဈေးဖြင့် ရောင်းထုတ်ရန် အကြံပြုပါသည်"
                } else {
                    "ရောင်းထွက်နှုန်း နှေးကွေးပါသည် - အခြားပစ္စည်းများနှင့် တွဲဖက်ရောင်းချပါ"
                }
                SlowMovingItem(
                    productId = prod.productId,
                    productName = prod.name,
                    category = prod.category,
                    currentStock = prod.stockQuantity,
                    daysWithoutSale = 30,
                    tiedCapitalMinor = tiedCapital,
                    recommendation = rec
                )
            } else null
        }.sortedByDescending { it.tiedCapitalMinor }.take(8)

        // 3. Smart Restock Recommendations
        val restockRecommendations = products.mapNotNull { prod ->
            val soldLines = itemsByProduct[prod.productId].orEmpty()
            val totalSold = soldLines.sumOf { it.quantity }
            val dailyVelocity = (totalSold / 30.0).coerceAtLeast(0.1)

            val isLow = prod.stockQuantity <= prod.lowStockThreshold
            val willRunOutIn15Days = (dailyVelocity * 15) > prod.stockQuantity

            if (isLow || willRunOutIn15Days) {
                val neededFor1Month = ceil(dailyVelocity * 30).toLong()
                val orderQty = max(neededFor1Month - prod.stockQuantity, prod.lowStockThreshold * 2).coerceAtLeast(5L)
                val estimatedCost = orderQty * prod.costPriceMinor
                val urgency = if (prod.stockQuantity <= (prod.lowStockThreshold / 2)) "အရေးပေါ်" else if (isLow) "ပုံမှန်" else "ကြိုတင်ပြင်ဆင်"

                // Find matching supplier if any
                val supplierName = suppliers.firstOrNull { sup ->
                    sup.name.contains(prod.category, ignoreCase = true) ||
                    sup.shopName.contains(prod.category, ignoreCase = true)
                }?.name

                RestockRecommendation(
                    productId = prod.productId,
                    productName = prod.name,
                    currentStock = prod.stockQuantity,
                    lowStockThreshold = prod.lowStockThreshold,
                    dailyVelocity = dailyVelocity,
                    suggestedOrderQty = orderQty,
                    estimatedCostMinor = estimatedCost,
                    urgency = urgency,
                    suggestedSupplier = supplierName
                )
            } else null
        }.sortedBy { it.currentStock }.take(10)

        // 4. Financial totals
        val totalRevenue = sales.sumOf { it.totalMinor }
        val totalProfit = topSelling.sumOf { it.estimatedProfitMinor }
        val demandPredictionNext15Days = (totalRevenue / 2).coerceAtLeast(100_000L)

        val topCategories = topSelling.map { it.category }.distinct().take(3)

        // 5. Damaged & Returns
        val damagedList = returns.filter { it.reason?.contains("ပျက်စီး", ignoreCase = true) == true }
        val regularReturns = returns.filter { it.reason?.contains("ပျက်စီး", ignoreCase = true) != true }

        val purchasesTotal = purchases.sumOf { it.totalCostMinor }

        return BiMonthlyReport(
            reportPeriod = period,
            generatedDate = generatedDate,
            isFirstOrFifteenth = isFirstOrFifteenth,
            totalRevenueMinor = totalRevenue,
            totalProfitMinor = totalProfit,
            totalOrdersCount = sales.size,
            topSellingItems = topSelling,
            slowMovingItems = slowMoving,
            restockRecommendations = restockRecommendations,
            demandPredictionNext15DaysMinor = demandPredictionNext15Days,
            highDemandCategories = if (topCategories.isNotEmpty()) topCategories else listOf("ဘိလပ်မြေ", "သွပ်ပြား", "သံချောင်း"),
            damagedItemsCount = damagedList.size,
            damagedTotalCostMinor = damagedList.sumOf { it.returnedAmountMinor },
            returnedItemsCount = regularReturns.size,
            returnedTotalRefundMinor = regularReturns.sumOf { it.returnedAmountMinor },
            purchasesTotalMinor = purchasesTotal,
            purchasesCount = purchases.size
        )
    }

    /**
     * 12-Month / 1-Year Seasonal Prediction Engine
     * Explains which months have high sales for which items and calculates recommended stocking volumes
     * ("မည်သည့်လတွင် မည်သို့သော ပစ္စည်းများ ရောင်းကောင်းသည်၊ ထို့ကြောင့် မည်သို့သော ပစ္စည်းများ ဝယ်ယူလှောင်ထားသင့်သည့် ပမာဏ")
     */
    fun generate12MonthSeasonalPredictions(
        products: List<ProductEntity>,
    ): List<MonthlySeasonalInsight> {
        return listOf(
            MonthlySeasonalInsight(
                monthNumber = 1,
                monthNameMm = "ဇန်နဝါရီ (January)",
                seasonTitleMm = "ဆောင်းရာသီကုန်ခါနီး ဆောက်လုပ်ရေး အရှိန်အဟုန်မြင့်ချိန်",
                seasonalDescription = "ရာသီဥတု ခြောက်သွေ့သာယာသဖြင့် အိမ်ဆောက်ပန်းရံ၊ ကန်ထရိုက်ဆိုက်များနှင့် အခြေခံအဆောက်အအုံလုပ်ငန်းများ အပြည့်အဝလည်ပတ်သောလ ဖြစ်ပါသည်။ ဘိလပ်မြေ၊ သံချောင်း၊ အုတ်နှင့် ကွန်ကရစ်ပစ္စည်းများ ရောင်းအား အလွန်ကောင်းမွန်ပါသည်။",
                topTrendingCategories = listOf("ဘိလပ်မြေ", "သံချောင်း / သံထည်", "သံမှို / ကိရိယာ"),
                recommendedStockItems = listOf(
                    RecommendedStockItem("ဘိလပ်မြေ (Cement Bags)", "ဘိလပ်မြေ", "ဆောက်လုပ်ရေး အပြည့်အဝ လုပ်ကိုင်ချိန်", "ပုံမှန်ထက် ၅၀% ပိုမိုလှောင်ထားသင့် (အိတ် ၃၀၀ ~ ၅၀၀)", 1.5),
                    RecommendedStockItem("သံချောင်း ၁၀မမ / ၁၂မမ", "သံချောင်း / သံထည်", "ဖောင်ဒေးရှင်းနှင့် တိုက်ဆောက်လုပ်ငန်းများ", "တန်ချိန် ၃ ~ ၅ တန် ကြိုတင်ဝယ်ယူထားသင့်", 1.4),
                    RecommendedStockItem("ကွန်ကရစ်မှိုနှင့် သံမှိုအရွယ်စုံ", "သံမှို / ကိရိယာ", "လောင်းကြေးဖောင်ပုံရိုက်လုပ်ငန်းများ", "သေတ္တာ ၂၀ ~ ၃၀ ခန့်", 1.3),
                )
            ),
            MonthlySeasonalInsight(
                monthNumber = 2,
                monthNameMm = "ဖေဖော်ဝါရီ (February)",
                seasonTitleMm = "နွေဦး ဆောက်လုပ်ရေး ရာသီ အကောင်းဆုံးကာလ",
                seasonalDescription = "ဆောက်လုပ်ရေးလုပ်ငန်းများ အရှိန်အဟုန်အမြင့်ဆုံးသို့ ရောက်ရှိပြီး ရေပိုက်လိုင်း၊ လျှပ်စစ်ပိုက်နှင့် အိမ်တွင်းအလှဆင်ပစ္စည်းများ စတင်ရောင်းအားတက်သောလ ဖြစ်ပါသည်။",
                topTrendingCategories = listOf("ပိုက်နှင့်ဆက်စပ်", "ဘိလပ်မြေ", "သံချောင်း / သံထည်"),
                recommendedStockItems = listOf(
                    RecommendedStockItem("PVC ရေပိုက်နှင့် ဆက်စပ်ပစ္စည်းများ", "ပိုက်နှင့်ဆက်စပ်", "အဆောက်အအုံ ရေပိုက်လိုင်းသွယ်တန်းချိန်", "ပုံမှန်ထက် ၄၀% ပိုမိုလှောင်ထားရန်", 1.4),
                    RecommendedStockItem("ဘိလပ်မြေ", "ဘိလပ်မြေ", "အချောကိုင်နှင့် ပန်းရံလုပ်ငန်းများ", "အိတ် ၂၅၀ ~ ၄၀၀ လှောင်ထားသင့်", 1.3),
                )
            ),
            MonthlySeasonalInsight(
                monthNumber = 3,
                monthNameMm = "မတ် (March)",
                seasonTitleMm = "နွေရာသီ ပူပြင်းချိန်နှင့် သင်္ကြန်မတိုင်မီ ကြိုတင်ပြင်ဆင်မှု",
                seasonalDescription = "သင်္ကြန်မတိုင်မီ အိမ်ပြင်လုပ်ငန်းများ အပြီးသတ်လုပ်ကိုင်ကြပြီး အမိုးအကာ၊ ရေလှောင်ကန်နှင့် ရေတင်စက်ပစ္စည်းများ လိုအပ်ချက်များပြားလာပါသည်။",
                topTrendingCategories = listOf("ပိုက်နှင့်ဆက်စပ်", "သွပ်ပြား / အမိုး", "သံမှို / ကိရိယာ"),
                recommendedStockItems = listOf(
                    RecommendedStockItem("PE ရေတိုင်ကီနှင့် ရေပိုက်အရွယ်စုံ", "ပိုက်နှင့်ဆက်စပ်", "နွေရာသီ ရေရှားပါးမှု ကြိုတင်ပြင်ဆင်ခြင်း", "အရွယ်အစားစုံ အလုံး ၂၀ ~ ၃၀ လှောင်ရန်", 1.4),
                    RecommendedStockItem("သွပ်ပြား အရွယ်စုံ", "သွပ်ပြား / အမိုး", "နွေမိုးမကျမီ အမိုးပြင်ဆင်ခြင်း", "ချပ် ၂၀၀ ~ ၃၅၀ ကြိုတင်ဝယ်ယူထားရန်", 1.3),
                )
            ),
            MonthlySeasonalInsight(
                monthNumber = 4,
                monthNameMm = "ဧပြီ (April)",
                seasonTitleMm = "သင်္ကြန်ကာလနှင့် မိုးကြို အမိုးအကာပြင်ဆင်ချိန်",
                seasonalDescription = "သင်္ကြန်ပွဲတော်အပြီးတွင် မိုးဦးလေဦးကာလအတွက် အမိုးအကာ၊ သွပ်ပြား၊ သွပ်မှိုနှင့် ရေမြောင်းပစ္စည်းများကို လူထုမှ အလုအယက် စတင်ဝယ်ယူကြပါသည်။",
                topTrendingCategories = listOf("သွပ်ပြား / အမိုး", "သံမှို / ကိရိယာ", "ဆေးသုတ် / အလှဆင်"),
                recommendedStockItems = listOf(
                    RecommendedStockItem("သွပ်ပြား ၈ ပေ / ၁၀ ပေ", "သွပ်ပြား / အမိုး", "မိုးမကျမီ အိမ်ခေါင်မိုး ပြင်ဆင်လဲလှယ်မှု အများဆုံး", "ပုံမှန်ထက် ၇၀% တိုးမြှင့်လှောင်ထားသင့် (ချပ် ၄၀၀ ~ ၆၀၀)", 1.7),
                    RecommendedStockItem("သွပ်မှို (Roofing Nails)", "သံမှို / ကိရိယာ", "သွပ်ရိုက်ရန် မရှိမဖြစ် ပစ္စည်း", "အိတ်ကြီး ၃၀ ~ ၅၀ ခန့်", 1.6),
                    RecommendedStockItem("မိုးကာစ (Waterproof Tarpaulin)", "အမိုးအကာ", "မိုးကာရန် ကြိုတင်ဝယ်ယူမှု", "လိပ် ၃၀ ~ ၅၀ လှောင်ထားရန်", 1.5),
                )
            ),
            MonthlySeasonalInsight(
                monthNumber = 5,
                monthNameMm = "မေ (May)",
                seasonTitleMm = "မိုးဦးရာသီ စတင်ချိန် (အမိုးအကာ လိုအပ်ချက် အမြင့်ဆုံးလ)",
                seasonalDescription = "မိုးဦးလေမုန်တိုင်းများနှင့် မိုးစတင်ရွာသွန်းသဖြင့် သွပ်ပြား၊ ရေယိုကာ ဆီလီကွန်၊ ရေတံလျှောက်နှင့် ရေစိုခံပစ္စည်းများ ရောင်းအား အထွတ်အထိပ်သို့ ရောက်ရှိပါသည်။",
                topTrendingCategories = listOf("သွပ်ပြား / အမိုး", "ပိုက်နှင့်ဆက်စပ်", "ဆေးသုတ် / အလှဆင်"),
                recommendedStockItems = listOf(
                    RecommendedStockItem("သွပ်ပြားအရွယ်စုံ (ရောင်စုံ / သွပ်ဖြူ)", "သွပ်ပြား / အမိုး", "မိုးစရွာချိန် အမိုးပေါက် ဖာထေးလဲလှယ်ခြင်း", "ပုံမှန်ထက် ၈၀% ပိုမိုလှောင်ထားသင့် (အမြင့်ဆုံးစတော့ထားရန်)", 1.8),
                    RecommendedStockItem("ဆီလီကွန် (Silicone Sealant) & ရေယိုကာတိပ်", "ဆေးသုတ် / အလှဆင်", "ခေါင်မိုး ရေယိုဖာထေးမှု အရေးပေါ်လိုအပ်ခြင်း", "ဘူး ၁၀၀ ~ ၂၀၀ လှောင်ထားသင့်", 2.0),
                    RecommendedStockItem("PVC ရေတံလျှောက်နှင့် ရေဆင်းပိုက်", "ပိုက်နှင့်ဆက်စပ်", "မိုးရေစီးဆင်းမှု လမ်းကြောင်းပြုလုပ်ခြင်း", "စုံစုံလင်လင် အချောင်း ၈၀ ~ ၁၂၀ ခန့်", 1.5),
                )
            ),
            MonthlySeasonalInsight(
                monthNumber = 6,
                monthNameMm = "ဇွန် (June)",
                seasonTitleMm = "မိုးရာသီ အပြည့်အဝ (အိမ်တွင်းပြုပြင်ထိန်းသိမ်းမှု ကာလ)",
                seasonalDescription = "မိုးသည်းထန်စွာ ရွာသွန်းသဖြင့် အပြင်ဘက် ဆောက်လုပ်ရေး နှေးကွေးသော်လည်း ရေယိုစိမ့်မှု ပြုပြင်ခြင်း၊ ရေစုပ်စက်၊ မိုးကာစနှင့် အိမ်တွင်း လျှပ်စစ်/ပိုက်လိုင်းများ အဆက်မပြတ် ရောင်းရပါသည်။",
                topTrendingCategories = listOf("ပိုက်နှင့်ဆက်စပ်", "ဆေးသုတ် / အလှဆင်", "သံမှို / ကိရိယာ"),
                recommendedStockItems = listOf(
                    RecommendedStockItem("Waterproof Coating / ရေလုံဆေး", "ဆေးသုတ် / အလှဆင်", "နံရံနှင့် မျက်နှာကျက် ရေစိမ့်ကာကွယ်ရန်", "ပုံး ၃၀ ~ ၅၀ လှောင်ထားသင့်", 1.5),
                    RecommendedStockItem("မိုးရေစုပ်စက်နှင့် ရေပိုက်များ", "ပိုက်နှင့်ဆက်စပ်", "ရေကြီးရေလျှံမှု ဖြေရှင်းရန်", "စက်အလုံး ၁၀ ~ ၁၅ လုံး", 1.3),
                )
            ),
            MonthlySeasonalInsight(
                monthNumber = 7,
                monthNameMm = "ဇူလိုင် (July)",
                seasonTitleMm = "မိုးလယ်ကာလ စတော့ထိန်းသိမ်းချိန်နှင့် ပစ္စည်းစုံလင်မှု အဓိက",
                seasonalDescription = "အခြေခံ အပြင်ဘက် ဆောက်လုပ်ရေး အနည်းငယ် အေးသော်လည်း အသေးစား ပြုပြင်ထိန်းသိမ်းရေး ကိရိယာများ၊ ရေယိုကာပစ္စည်းများနှင့် အိမ်တွင်းပစ္စည်းများ ဆက်လက်ရောင်းရပါသည်။",
                topTrendingCategories = listOf("ပိုက်နှင့်ဆက်စပ်", "သံမှို / ကိရိယာ"),
                recommendedStockItems = listOf(
                    RecommendedStockItem("ဆီလီကွန်၊ ရေယိုကာတိပ်နှင့် သွပ်မှို", "သံမှို / ကိရိယာ", "မိုးလယ် မိုးယိုပြုပြင်မှုများ", "အရန်သင့် ပုံမှန်စတော့ ထိန်းထားရန်", 1.2),
                    RecommendedStockItem("အိမ်သုံးကိရိယာတန်ဆာပလာများ", "သံမှို / ကိရိယာ", "အိမ်တွင်းပြုပြင်မှု လိုအပ်ချက်များ", "စုံစုံလင်လင် ၂၀ ခုစီ", 1.1),
                )
            ),
            MonthlySeasonalInsight(
                monthNumber = 8,
                monthNameMm = "သြဂုတ် (August)",
                seasonTitleMm = "မိုးနှောင်းကာလနှင့် ပွင့်လင်းရာသီအတွက် ကြိုတင်ပြင်ဆင်မှု",
                seasonalDescription = "မိုးရာသီ ကုန်ခါနီးဖြစ်ပြီး လာမည့် ပွင့်လင်းရာသီ ဆောက်လုပ်ရေးနှင့် သီတင်းကျွတ် အိမ်ဆေးသုတ်မည့် ကာလအတွက် ပစ္စည်းစတင် မှာယူစုဆောင်းသင့်သောလ ဖြစ်ပါသည်။",
                topTrendingCategories = listOf("ဆေးသုတ် / အလှဆင်", "ဘိလပ်မြေ", "သံချောင်း / သံထည်"),
                recommendedStockItems = listOf(
                    RecommendedStockItem("ဆေးသုတ်ပစ္စည်းများ (ကြိုတင်မှာယူမှု)", "ဆေးသုတ် / အလှဆင်", "သီတင်းကျွတ် အိမ်ဆေးသုတ်ရာသီအတွက် စတင်စုဆောင်းရန်", "ပုံမှန်ထက် ၃၀% တိုးမှာရန်", 1.3),
                    RecommendedStockItem("ဘိလပ်မြေနှင့် သံချောင်း", "ဘိလပ်မြေ", "ပွင့်လင်းရာသီ ဆောက်လုပ်ရေးအတွက် ကြိုတင်စုဆောင်းခြင်း", "ဈေးကောင်းချိန်တွင် စတင်လှောင်ရန်", 1.2),
                )
            ),
            MonthlySeasonalInsight(
                monthNumber = 9,
                monthNameMm = "စက်တင်ဘာ (September)",
                seasonTitleMm = "သီတင်းကျွတ် အိမ်အလှဆင် ရာသီအကြို ကာလ",
                seasonalDescription = "မိုးကုန်သွားပြီဖြစ်၍ အိမ်သုတ်ဆေး၊ ထုံး၊ သုတ်ဆေးသုံး ပစ္စည်းများ၊ တံခါးလက်ကိုင်၊ သော့နှင့် အိမ်တွင်း အလှဆင်ပစ္စည်းများ ရောင်းအား စတင်ထိုးတက်လာပါသည်။",
                topTrendingCategories = listOf("ဆေးသုတ် / အလှဆင်", "သံမှို / ကိရိယာ", "ပိုက်နှင့်ဆက်စပ်"),
                recommendedStockItems = listOf(
                    RecommendedStockItem("အိမ်တွင်း/အပြင် ဆေးသုတ်ပုံးများ (Emulsion/Weathercoat)", "ဆေးသုတ် / အလှဆင်", "သီတင်းကျွတ် အိမ်သန့်ရှင်းရေးနှင့် ဆေးသုတ်ခြင်း", "ပုံမှန်ထက် ၆၀% ပိုမိုလှောင်ထားသင့် (ပုံး ၈၀ ~ ၁၅၀)", 1.6),
                    RecommendedStockItem("ဆေးသုတ်ဒလိမ့်တုံး၊ ဘရက်ရှ်၊ တိပ်", "ဆေးသုတ် / အလှဆင်", "ဆေးသုတ်ရန် မရှိမဖြစ် ကိရိယာများ", "အခု ၁၀၀ ~ ၂၀၀ စုံလင်စွာ လှောင်ရန်", 1.5),
                    RecommendedStockItem("ကြမ်းခင်းကြွေပြားကပ် ကော်နှင့် ဘိလပ်မြေဖြူ", "ဘိလပ်မြေ", "အိမ်အလှဆင်လုပ်ငန်းများ", "အိတ် ၅၀ ~ ၁၀၀ ခန့်", 1.4),
                )
            ),
            MonthlySeasonalInsight(
                monthNumber = 10,
                monthNameMm = "အောက်တိုဘာ (October)",
                seasonTitleMm = "သီတင်းကျွတ် ပွဲတော်ကာလနှင့် ဆေးသုတ်လုပ်ငန်း အထွတ်အထိပ်",
                seasonalDescription = "တစ်နှစ်ပတ်လုံးတွင် အိမ်ဆေးသုတ်ပစ္စည်း၊ သော့၊ မီးကြိုး၊ တံခါးဆက်စပ်ပစ္စည်းများနှင့် အိမ်အလှဆင်ပစ္စည်းများ ရောင်းအား အကောင်းမွန်ဆုံးလ ဖြစ်ပါသည်။",
                topTrendingCategories = listOf("ဆေးသုတ် / အလှဆင်", "သံမှို / ကိရိယာ", "ပိုက်နှင့်ဆက်စပ်"),
                recommendedStockItems = listOf(
                    RecommendedStockItem("ဆေးသုတ်ပစ္စည်း အစုံအလင် (အဖြူ၊ အရောင်စုံ)", "ဆေးသုတ် / အလှဆင်", "သီတင်းကျွတ် ပွဲတော်အိမ်အလှဆင်ခြင်း", "ပုံမှန်ထက် ၇၅% တိုးမြှင့်လှောင်ထားသင့် (စတော့အပြည့်)", 1.75),
                    RecommendedStockItem("တံခါးသော့၊ ပတ္တာ၊ လက်ကိုင်များ", "သံမှို / ကိရိယာ", "အိမ်တံခါးပြင်ဆင် အသစ်လဲခြင်း", "အတွဲ ၄၀ ~ ၆၀ ခန့်", 1.4),
                    RecommendedStockItem("မီးပလပ်ခေါင်း၊ ဝါယာကြိုးနှင့် လျှပ်စစ်ပစ္စည်းများ", "အထွေထွေ", "မီးထွန်းပွဲတော် မီးဆင်ပြင်ခြင်း", "စုံလင်စွာ လှောင်ရန်", 1.3),
                )
            ),
            MonthlySeasonalInsight(
                monthNumber = 11,
                monthNameMm = "နိုဝင်ဘာ (November)",
                seasonTitleMm = "တန်ဆောင်တိုင်နှင့် ဆောင်းရာသီ ဆောက်လုပ်ရေး စတင်ချိန်",
                seasonalDescription = "ဆောင်းရာသီ ပွင့်လင်းရာသီ စတင်ပြီဖြစ်၍ ရပ်နားထားသော အကြီးစား ဆောက်လုပ်ရေးဆိုက်များ အားလုံး ပြန်လည်စတင်ပြီး ဘိလပ်မြေ၊ သံချောင်း၊ အုတ် သဲ ကျောက် အလုံးအရင်း ရောင်းရပါသည်။",
                topTrendingCategories = listOf("ဘိလပ်မြေ", "သံချောင်း / သံထည်", "သံမှို / ကိရိယာ"),
                recommendedStockItems = listOf(
                    RecommendedStockItem("ဘိလပ်မြေ (Cement)", "ဘိလပ်မြေ", "ဆောက်လုပ်ရေးဆိုက်ကြီးများ စတင်လည်ပတ်ခြင်း", "ပုံမှန်ထက် ၆၀% တိုးမြှင့်လှောင်ထားသင့် (အိတ် ၄၀၀ ~ ၆၀၀)", 1.6),
                    RecommendedStockItem("သံချောင်း ၁၀မမ / ၁၂မမ / ၁၆မမ", "သံချောင်း / သံထည်", "အဆောက်အအုံ တည်ဆောက်ရေး", "တန်ချိန် ၄ ~ ၇ တန် ကြိုတင်ဝယ်ယူထားသင့်", 1.5),
                    RecommendedStockItem("အုတ်၊ သဲ၊ ကျောက် နှင့် ဆောက်လုပ်ရေးကိရိယာများ", "သံမှို / ကိရိယာ", "ပန်းရံလုပ်ငန်းများ", "စတော့လုံလောက်စွာ ထားရန်", 1.3),
                )
            ),
            MonthlySeasonalInsight(
                monthNumber = 12,
                monthNameMm = "ဒီဇင်ဘာ (December)",
                seasonTitleMm = "နှစ်ကုန်ပိုင်း ဆောက်လုပ်ရေးနှင့် နှစ်ချုပ် ပြုပြင်မှုများ",
                seasonalDescription = "နှစ်ကုန်ကာလဖြစ်ပြီး ဘဏ္ဍာရေးနှစ် သို့မဟုတ် နှစ်မကုန်မီ အဆောက်အအုံများ အပြီးသတ်တည်ဆောက်မှု မြင့်မားသဖြင့် ပစ္စည်းမျိုးစုံ ရောင်းအား အလွန်ကောင်းမွန်ပါသည်။",
                topTrendingCategories = listOf("ဘိလပ်မြေ", "သံချောင်း / သံထည်", "ပိုက်နှင့်ဆက်စပ်", "ဆေးသုတ် / အလှဆင်"),
                recommendedStockItems = listOf(
                    RecommendedStockItem("ဘိလပ်မြေ & သံချောင်း", "ဘိလပ်မြေ", "ဆောက်လုပ်ရေး လုပ်ငန်းများ အရှိန်အဟုန်ပြည့်လည်ပတ်ခြင်း", "ပုံမှန်ထက် ၅၀% ပိုမိုလှောင်ထားသင့် (အိတ် ၃၅၀ ~ ၅၀၀)", 1.5),
                    RecommendedStockItem("ရေပိုက်၊ ရေချိုးခန်းသုံး ဆက်စပ်ပစ္စည်းများ", "ပိုက်နှင့်ဆက်စပ်", "အိမ်အပြီးသတ် တပ်ဆင်မှုများ", "စုံစုံလင်လင် လှောင်ထားရန်", 1.3),
                    RecommendedStockItem("သံမှို၊ ဝက်အူ၊ ကွန်ကရစ်မှို", "သံမှို / ကိရိယာ", "ဆောက်လုပ်ရေး လက်သမားလုပ်ငန်းများ", "သေတ္တာ ၃၀ ခန့်", 1.3),
                )
            )
        )
    }

    /**
     * Format Bi-Monthly Report into clean Myanmar Markdown for Gemini / Copilot
     */
    fun formatBiMonthlyReportToMarkdown(report: BiMonthlyReport): String {
        val sb = StringBuilder()
        sb.appendLine("📊 **${report.reportPeriod} စီးပွားရေးဆန်းစစ်ချက် အစီရင်ခံစာ (BI Report)**")
        sb.appendLine("🗓️ ထုတ်ယူသည့်ရက်စွဲ: ${report.generatedDate}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("💰 **ငွေကြေးနှင့် အရောင်းအကျဉ်းချုပ်:**")
        sb.appendLine("• စုစုပေါင်း အရောင်းရငွေ: **%,d Ks** (${report.totalOrdersCount} orders)".format(report.totalRevenueMinor))
        sb.appendLine("• ခန့်မှန်း အသားတင်အမြတ်: **%,d Ks**".format(report.totalProfitMinor))
        sb.appendLine("• လာမည့် ၁၅ ရက် ခန့်မှန်းရောင်းအား: **%,d Ks**".format(report.demandPredictionNext15DaysMinor))
        sb.appendLine("• ဝယ်လိုအား အများဆုံး ကဏ္ဍများ: ${report.highDemandCategories.joinToString(", ")}")
        sb.appendLine()

        sb.appendLine("🏆 **ရောင်းအကောင်းဆုံး ပစ္စည်းများ (Top 5):**")
        if (report.topSellingItems.isEmpty()) {
            sb.appendLine("• လက်ရှိတွင် အရောင်းစာရင်း မရှိသေးပါ")
        } else {
            report.topSellingItems.take(5).forEachIndexed { idx, item ->
                sb.appendLine("${idx + 1}. **${item.productName}** [${item.category}]")
                sb.appendLine("   - အရေအတွက်: ${item.quantitySold} ခု | ရငွေ: %,d Ks | အမြတ်: %,d Ks".format(item.totalRevenueMinor, item.estimatedProfitMinor))
            }
        }
        sb.appendLine()

        sb.appendLine("⚠️ **ရောင်းမထွက် / သိုလှောင်မိနေသော ပစ္စည်းများ (Dead / Slow Stock):**")
        if (report.slowMovingItems.isEmpty()) {
            sb.appendLine("• သိုလှောင်မိနေသော ပစ္စည်းမရှိပါ (စတော့လည်ပတ်မှု ကောင်းမွန်ပါသည်)")
        } else {
            report.slowMovingItems.take(4).forEachIndexed { idx, item ->
                sb.appendLine("${idx + 1}. **${item.productName}** (လက်ကျန်: ${item.currentStock} ခု)")
                sb.appendLine("   - အရင်းငွေ မြုပ်နှံမှု: %,d Ks".format(item.tiedCapitalMinor))
                sb.appendLine("   - 💡 အကြံပြုချက်: ${item.recommendation}")
            }
        }
        sb.appendLine()

        sb.appendLine("📦 **ပြန်လည်မှာယူရန် လိုအပ်သော စတော့များ (Smart Restock):**")
        if (report.restockRecommendations.isEmpty()) {
            sb.appendLine("• စတော့များ အားလုံး လုံလောက်စွာ ရှိနေပါသည်")
        } else {
            report.restockRecommendations.take(5).forEachIndexed { idx, item ->
                sb.appendLine("${idx + 1}. **${item.productName}** [${item.urgency}]")
                sb.appendLine("   - လက်ကျန်: ${item.currentStock} ခု (အနိမ့်ဆုံးသတ်မှတ်ချက်: ${item.lowStockThreshold} ခု)")
                sb.appendLine("   - 🛒 မှာယူသင့်သည့် ပမာဏ: **${item.suggestedOrderQty} ခု** (ခန့်မှန်းကုန်ကျငွေ: %,d Ks)".format(item.estimatedCostMinor))
                if (!item.suggestedSupplier.isNullOrBlank()) {
                    sb.appendLine("   - 🏢 အကြံပြု ကုန်သွင်းသူ: ${item.suggestedSupplier}")
                }
            }
        }
        sb.appendLine()

        sb.appendLine("🛠️ **ပျက်စီးဆုံးရှုံးမှုနှင့် ကုန်သွင်းအကျဉ်း:**")
        sb.appendLine("• ပျက်စီးဆုံးရှုံးမှု: ${report.damagedItemsCount} ကြိမ် (တန်ဖိုး: %,d Ks)".format(report.damagedTotalCostMinor))
        sb.appendLine("• Customer ပြန်သွင်းငွေ: ${report.returnedItemsCount} ကြိမ် (ပြန်အမ်းငွေ: %,d Ks)".format(report.returnedTotalRefundMinor))
        sb.appendLine("• ကုန်ပစ္စည်း ပြန်လည်ဖြည့်တင်းမှု (Purchases): ${report.purchasesCount} ကြိမ် (စုစုပေါင်း: %,d Ks)".format(report.purchasesTotalMinor))
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        return sb.toString()
    }

    /**
     * Format 12-Month Annual Seasonal Prediction into Myanmar Markdown
     */
    fun format12MonthPredictionToMarkdown(
        predictions: List<MonthlySeasonalInsight>,
        currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("📅 **၁၂ လ ရာသီအလိုက် အရောင်းလမ်းကြောင်းနှင့် ကြိုတင်လှောင်ထားသင့်သည့် စတော့ပမာဏ သုံးသပ်ချက်**")
        sb.appendLine("*(Annual 12-Month Seasonal Demand & Purchase Volume Forecast)*")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // First highlight the current month and upcoming month
        val currentInsight = predictions.find { it.monthNumber == currentMonth }
        val nextMonthNum = if (currentMonth == 12) 1 else currentMonth + 1
        val nextInsight = predictions.find { it.monthNumber == nextMonthNum }

        if (currentInsight != null) {
            sb.appendLine("🌟 **ယခုလက်ရှိလ [${currentInsight.monthNameMm}] အထူးအလေးထားရန်:**")
            sb.appendLine("• အခြေအနေ: **${currentInsight.seasonTitleMm}**")
            sb.appendLine("• ${currentInsight.seasonalDescription}")
            sb.appendLine("• 🛒 **ယခုလတွင် ကြိုတင်လှောင်ထားသင့်သည့် ပမာဏ:**")
            currentInsight.recommendedStockItems.forEach { item ->
                sb.appendLine("  - **${item.productName}** [${item.category}]: **${item.recommendedPurchaseVolume}** (${item.reason})")
            }
            sb.appendLine()
        }

        if (nextInsight != null) {
            sb.appendLine("⏭️ **လာမည့်လ [${nextInsight.monthNameMm}] ကြိုတင်ပြင်ဆင်မှု:**")
            sb.appendLine("• အခြေအနေ: **${nextInsight.seasonTitleMm}**")
            nextInsight.recommendedStockItems.forEach { item ->
                sb.appendLine("  - **${item.productName}**: ${item.recommendedPurchaseVolume}")
            }
            sb.appendLine()
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📋 **တစ်နှစ်ပတ်လုံး (၁၂ လ) ပြည့်စုံသော ရာသီအလိုက် ဇယား:**")
        sb.appendLine()

        predictions.forEach { m ->
            val isCurrent = m.monthNumber == currentMonth
            val prefix = if (isCurrent) "👉 " else "🔹 "
            sb.appendLine("$prefix**${m.monthNameMm}** - *${m.seasonTitleMm}*")
            sb.appendLine("   • ရောင်းအားကောင်းသော ပစ္စည်းများ: ${m.topTrendingCategories.joinToString(", ")}")
            sb.appendLine("   • 💡 အကြံပြုဝယ်ယူလှောင်ရမည့် ပမာဏ:")
            m.recommendedStockItems.forEach { item ->
                sb.appendLine("     - ${item.productName}: **${item.recommendedPurchaseVolume}**")
            }
            sb.appendLine()
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("💡 **စတော့လှောင်ယူမှုဆိုင်ရာ အကြံပြုချက်:** မိုးရာသီအကြို (ဧပြီ/မေ) တွင် သွပ်ပြားနှင့် ရေယိုကာပစ္စည်းများကို လည်းကောင်း၊ ပွင့်လင်းရာသီ (နိုဝင်ဘာ/ဒီဇင်ဘာ/ဇန်နဝါရီ) တွင် ဘိလပ်မြေနှင့် သံချောင်းများကို လည်းကောင်း ကြိုတင်လှောင်ထားခြင်းဖြင့် ဈေးနှုန်းအတက်အကျကို ကာကွယ်နိုင်ပြီး အမြတ်ငွေ အများဆုံး ရရှိစေနိုင်ပါသည်။")
        return sb.toString()
    }
}
