package com.newdubai.shop.ui

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.newdubai.shop.data.*
import com.newdubai.shop.util.Prefs
import com.newdubai.shop.util.Snapshot
import com.newdubai.shop.util.Snapshots
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppVM(app: Application) : AndroidViewModel(app) {
    val db = AppDb.get(app)
    val prefs = Prefs(app)

    var lang by mutableStateOf(prefs.lang)
    var inch by mutableStateOf(prefs.unit == "in")
    var themeMode by mutableStateOf(prefs.themeMode)
    var locked by mutableStateOf(true)
    var fails = 0
    var lockedUntil = 0L

    private fun <T> flowState(f: kotlinx.coroutines.flow.Flow<List<T>>) =
        f.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customers = flowState(db.customers().flow())
    val orders = flowState(db.orders().flow())
    val payments = flowState(db.payments().flow())
    val fabrics = flowState(db.fabrics().flow())
    val purchases = flowState(db.purchases().flow())
    val sales = flowState(db.sales().flow())
    val expenses = flowState(db.expenses().flow())
    val measurements = flowState(db.measurements().flow())

    init {
        // daily automatic backup on app start (in addition to the scheduled worker)
        viewModelScope.launch {
            runCatching { if (prefs.autoBackup) Snapshots.writeAuto(app, Snapshots.build(db)) }
        }
    }

    fun changeLang(l: String) { lang = l; prefs.lang = l }
    fun changeInch(b: Boolean) { inch = b; prefs.unit = if (b) "in" else "cm" }
    fun changeTheme(mode: String) { themeMode = mode; prefs.themeMode = mode }

    fun tryUnlock(secret: String, isPin: Boolean): String? {
        val now = SystemClock.elapsedRealtime()
        if (now < lockedUntil) return "lockwait"
        if (prefs.verify(secret, isPin)) { fails = 0; locked = false; return null }
        fails++
        if (fails >= 5) { fails = 0; lockedUntil = now + 30_000; return "lockwait" }
        return "wrong"
    }

    private fun tx(done: (String?) -> Unit = {}, block: suspend () -> String?) {
        viewModelScope.launch {
            val r = try { db.withTransaction { block() } } catch (e: Exception) { "err_db" }
            done(r)
        }
    }

    /* customers */
    fun saveCustomer(c: Customer, done: (String?) -> Unit) = tx(done) {
        if (c.firstName.isBlank()) return@tx "req"
        val old = if (c.id != 0L) db.customers().list().find { it.id == c.id } else null
        val normalizedPhone = Calc.normalizeDigits(c.phone)
        if (!Calc.isAfghanMobile(normalizedPhone)) return@tx "badphone"
        if (db.customers().list().any { it.id != c.id && Calc.normalizeDigits(it.phone) == normalizedPhone }) return@tx "phoneexists"
        db.customers().upsert(c.copy(phone = normalizedPhone)); null
    }

    fun deleteCustomer(c: Customer, done: (String?) -> Unit) = tx(done) {
        if (db.orders().list().any { it.customerId == c.id }) return@tx "hasord"
        db.measurements().list().filter { it.customerId == c.id }.forEach { db.measurements().delete(it) }
        db.customers().delete(c); null
    }

    /* orders */
    fun saveOrder(o: TailorOrder, advance: Double, meas: MeasurementRec?, done: (String?) -> Unit) = tx(done) {
        if (o.customerId == 0L) return@tx "customer"
        if (db.customers().list().none { it.id == o.customerId }) return@tx "customer"
        val garmentText = if (o.type.startsWith("m_custom:") || o.type.startsWith("c_custom:")) o.type.substringAfterLast(':') else o.type
        if (garmentText.isBlank()) return@tx "req"
        if (runCatching { LocalDate.parse(o.orderDate); LocalDate.parse(o.deliveryDate) }.isFailure ||
            o.deliveryDate < o.orderDate) return@tx "req"
        if (!o.tailorPrice.isFinite() || !o.fabricPrice.isFinite() || !advance.isFinite() ||
            o.tailorPrice < 0 || o.fabricPrice < 0 || advance < 0) return@tx "req"
        if (o.fabricSource !in setOf("shop", "customer", "own")) return@tx "req"
        val old = if (o.id != 0L) db.orders().list().find { it.id == o.id } else null
        val fabs = db.fabrics().list().associateBy { it.id }.toMutableMap()
        if (old != null && old.fabricSource == "shop") {
            fabs[old.fabricId]?.let { fabs[it.id] = it.copy(stock = it.stock + old.meters) }
        }
        var order = o
        if (o.fabricSource == "shop") {
            val f = fabs[o.fabricId] ?: return@tx "fname"
            if (o.meters <= 0) return@tx "meters"
            if (f.stock < o.meters - 1e-9) return@tx "stockerr"
            fabs[f.id] = f.copy(stock = f.stock - o.meters)
        } else order = o.copy(fabricId = 0, meters = 0.0, fabricPrice = 0.0)
        val total = order.fabricPrice + order.tailorPrice
        val pays = db.payments().list()
        if (old != null) { if (total < Calc.paid(old, pays) - 1e-9) return@tx "overpay" }
        else if (advance > total + 1e-9) return@tx "overpay"
        fabs.values.forEach { db.fabrics().upsert(it) }
        val no = old?.no ?: run {
            val n = (db.orders().list().maxOfOrNull { it.no.removePrefix("ND-").toIntOrNull() ?: 0 } ?: 0) + 1
            "ND-" + n.toString().padStart(4, '0')
        }
        val id = db.orders().upsert(order.copy(no = no))
        val oid = if (order.id != 0L) order.id else id
        if (old != null && old.customerId != order.customerId) {
            db.payments().list().filter { it.orderId == oid }.forEach { payment ->
                db.payments().upsert(payment.copy(customerId = order.customerId))
            }
        }
        if (old == null && advance > 0) {
            db.payments().upsert(Payment(orderId = oid, customerId = order.customerId, amount = advance, date = order.orderDate, kind = "adv"))
        }
        if (meas != null) db.measurements().upsert(meas.copy(orderNo = no))
        null
    }

    fun deleteOrder(o: TailorOrder) = tx {
        if (o.fabricSource == "shop") {
            db.fabrics().list().find { it.id == o.fabricId }?.let { db.fabrics().upsert(it.copy(stock = it.stock + o.meters)) }
        }
        db.payments().list().filter { it.orderId == o.id }.forEach { db.payments().delete(it) }
        db.orders().delete(o); null
    }

    fun setStatus(o: TailorOrder, s: String) = tx { db.orders().upsert(o.copy(status = s)); null }

    fun addPayment(orderId: Long, amount: Double, date: String, done: (String?) -> Unit) = tx(done) {
        val o = db.orders().list().find { it.id == orderId } ?: return@tx "err_db"
        if (!amount.isFinite() || amount <= 0 || runCatching { LocalDate.parse(date) }.isFailure) return@tx "amt"
        val rem = Calc.balance(o, db.payments().list())
        if (amount > rem + 0.001) return@tx "overpay"
        db.payments().upsert(Payment(orderId = o.id, customerId = o.customerId, amount = amount, date = date,
            kind = if (rem - amount < 0.01) "full" else "part"))
        null
    }

    /* measurements */
    fun saveMeasurement(m: MeasurementRec, done: (String?) -> Unit) = tx(done) {
        if (m.customerId == 0L || db.customers().list().none { it.id == m.customerId }) return@tx "customer"
        if (m.vals.isBlank() || m.vals.split(",").all { it.isBlank() }) return@tx "req"
        val measurementType = if (m.type.startsWith("m_custom:") || m.type.startsWith("c_custom:")) m.type.substringAfterLast(':') else m.type
        if (measurementType.isBlank()) return@tx "req"
        if (runCatching { LocalDate.parse(m.date) }.isFailure) return@tx "req"
        val rawValues = m.vals.split(",")
        if (rawValues.size > 12 || rawValues.any { it.isNotBlank() && it.trim().toDoubleOrNull() == null }) return@tx "badmeasure"
        val values = rawValues.filter { it.isNotBlank() }.map { it.trim().toDouble() }
        if (values.any { !it.isFinite() || it <= 0.0 || it > 500.0 }) return@tx "badmeasure"
        db.measurements().upsert(m.copy(vals = rawValues.joinToString(",") { it.trim() })); null
    }
    fun deleteMeasurement(m: MeasurementRec) = tx { db.measurements().delete(m); null }

    /* fabric */
    fun saveFabric(f: Fabric, initialStock: Double, done: (String?) -> Unit) = tx(done) {
        if (f.name.isBlank()) return@tx "req"
        if (!f.buyPrice.isFinite() || !f.sellPrice.isFinite() || !f.stock.isFinite() ||
            f.buyPrice < 0 || f.sellPrice < 0 || f.stock < 0 || !initialStock.isFinite() || initialStock < 0) return@tx "req"
        val id = db.fabrics().upsert(f)
        if (f.id == 0L && initialStock > 0) {
            db.purchases().upsert(Purchase(fabricId = id, meters = initialStock, pricePerMeter = f.buyPrice, date = LocalDate.now().toString()))
        }
        null
    }

    fun deleteFabric(f: Fabric, done: (String?) -> Unit) = tx(done) {
        if (db.orders().list().any { it.fabricId == f.id }) return@tx "hasord"
        db.purchases().list().filter { it.fabricId == f.id }.forEach { db.purchases().delete(it) }
        db.sales().list().filter { it.fabricId == f.id }.forEach { db.sales().delete(it) }
        db.fabrics().delete(f); null
    }

    fun buyFabric(f: Fabric, meters: Double, ppm: Double, date: String, done: (String?) -> Unit) = tx(done) {
        if (!meters.isFinite() || !ppm.isFinite() || meters <= 0 || ppm < 0 ||
            runCatching { LocalDate.parse(date) }.isFailure) return@tx "meters"
        val cur = db.fabrics().list().find { it.id == f.id } ?: return@tx "err_db"
        if (!(cur.stock + meters).isFinite()) return@tx "meters"
        db.fabrics().upsert(cur.copy(stock = cur.stock + meters, buyPrice = ppm))
        db.purchases().upsert(Purchase(fabricId = f.id, meters = meters, pricePerMeter = ppm, date = date)); null
    }

    fun sellFabric(f: Fabric, meters: Double, ppm: Double, date: String, done: (String?) -> Unit) = tx(done) {
        if (!meters.isFinite() || !ppm.isFinite() || meters <= 0 || ppm < 0 ||
            runCatching { LocalDate.parse(date) }.isFailure) return@tx "meters"
        val cur = db.fabrics().list().find { it.id == f.id } ?: return@tx "err_db"
        if (cur.stock < meters - 1e-9) return@tx "stockerr"
        db.fabrics().upsert(cur.copy(stock = cur.stock - meters))
        db.sales().upsert(Sale(fabricId = f.id, meters = meters, pricePerMeter = ppm, date = date)); null
    }

    /* expenses */
    fun saveExpense(e: Expense, done: (String?) -> Unit) = tx(done) {
        if (e.title.isBlank()) return@tx "req"
        if (!e.amount.isFinite() || e.amount <= 0 || runCatching { LocalDate.parse(e.date) }.isFailure) return@tx "amt"
        db.expenses().upsert(e); null
    }
    fun deleteExpense(e: Expense) = tx { db.expenses().delete(e); null }

    /* backup */
    suspend fun snapshot(): Snapshot = Snapshots.build(db)

    fun restore(s: Snapshot, done: (String?) -> Unit) {
        viewModelScope.launch {
            val r = try {
                // Validate the backup before touching the current database.
                val customerIds = s.customers.map { it.id }.toSet()
                val orderIds = s.orders.map { it.id }.toSet()
                val fabricIds = s.fabrics.map { it.id }.toSet()
                require(s.version == 1)
                require(s.customers.all { it.id > 0 && it.firstName.isNotBlank() })
                require(s.orders.all { it.id > 0 && it.customerId in customerIds })
                require(s.payments.all { it.id > 0 && it.orderId in orderIds && it.customerId in customerIds && it.amount.isFinite() && it.amount > 0 })
                require(s.measurements.all { it.id > 0 && it.customerId in customerIds })
                require(s.fabrics.all { it.id > 0 && it.stock.isFinite() && it.stock >= 0 && it.buyPrice.isFinite() && it.sellPrice.isFinite() })
                require(s.purchases.all { it.id > 0 && it.fabricId in fabricIds && it.meters.isFinite() && it.meters > 0 })
                require(s.sales.all { it.id > 0 && it.fabricId in fabricIds && it.meters.isFinite() && it.meters > 0 })
                require(s.expenses.all { it.id > 0 && it.amount.isFinite() && it.amount > 0 })
                Snapshots.writeAuto(getApplication(), Snapshots.build(db), "pre_restore")
                db.withTransaction {
                    // Clear dependent records first; inserts are atomic in this transaction.
                    db.payments().clear(); db.measurements().clear()
                    db.purchases().clear(); db.sales().clear()
                    db.orders().clear(); db.expenses().clear()
                    db.fabrics().clear(); db.customers().clear()
                    db.customers().insertAll(s.customers); db.fabrics().insertAll(s.fabrics)
                    db.orders().insertAll(s.orders); db.payments().insertAll(s.payments)
                    db.measurements().insertAll(s.measurements)
                    db.purchases().insertAll(s.purchases); db.sales().insertAll(s.sales)
                    db.expenses().insertAll(s.expenses)
                }
                null
            } catch (e: Exception) { "err_db" }
            done(r)
        }
    }
}
