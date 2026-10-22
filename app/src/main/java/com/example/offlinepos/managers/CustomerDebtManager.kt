package com.example.offlinepos.managers

import android.content.Context
import com.example.offlinepos.Sync.RecordLevelSyncManager
import com.example.offlinepos.data.local.AppDatabase
import com.example.offlinepos.data.local.CustomerDao
import com.example.offlinepos.data.local.CustomerEntity
import com.example.offlinepos.data.local.DebtTransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerDebtManager(
    private val customerDao: CustomerDao,
    private val db: AppDatabase,
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onTriggerSync: (String) -> Unit,
) {
    fun addCustomer(
        name: String,
        phone: String,
        address: String,
        creditLimit: Long,
        dueDateEpochMs: Long?,
        onResult: (Boolean, String) -> Unit,
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val customer = CustomerEntity(
                    name = name,
                    phone = phone,
                    address = address,
                    creditLimitMinor = creditLimit,
                    currentDebtMinor = 0L,
                    creditDueDateEpochMs = dueDateEpochMs,
                    createdAtEpochMs = System.currentTimeMillis()
                )
                customerDao.insertCustomer(customer)
                withContext(Dispatchers.Main) {
                    onTriggerSync("New Customer ($name)")
                    onResult(true, "ဝယ်သူ '$name' ကို စာရင်းသွင်းပြီးပါပြီ")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "ဝယ်သူသွင်းခြင်း မအောင်မြင်ပါ: ${e.message}")
                }
            }
        }
    }

    fun recordRepayment(
        customerId: Long,
        amount: Long,
        note: String,
        actorId: String,
        onResult: (Boolean, String) -> Unit,
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                customerDao.recordRepayment(customerId, amount, note, actorId)
                withContext(Dispatchers.Main) {
                    onTriggerSync("Debt Repayment")
                    onResult(true, "အကြွေးဆပ်ငွေ %,d Ks လက်ခံပြီးပါပြီ".format(amount))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "အကြွေးဆပ်ငွေမှတ်တမ်း မအောင်မြင်ပါ: ${e.message}")
                }
            }
        }
    }

    fun getCustomerTransactions(customerId: Long): Flow<List<DebtTransactionEntity>> {
        return customerDao.observeTransactionsForCustomer(customerId)
    }

    fun deleteCustomer(customerId: Long, onResult: (Boolean, String) -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                customerDao.deleteCustomer(customerId)
                RecordLevelSyncManager(context.applicationContext).recordTombstone(db, "customers", customerId.toString())
                withContext(Dispatchers.Main) {
                    onTriggerSync("Delete Customer")
                    onResult(true, "ဝယ်သူအချက်အလက်ကို အောင်မြင်စွာ ဖျက်ပစ်ပြီးပါပြီ")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "ဖျက်ပစ်ခြင်း မအောင်မြင်ပါ: ${e.message}")
                }
            }
        }
    }
}
