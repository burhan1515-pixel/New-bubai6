package com.newdubai.shop.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.newdubai.shop.data.TailorOrder
import java.io.File
import java.io.FileOutputStream

private fun createReceiptPdf(
    context: Context,
    order: TailorOrder,
    customer: String,
    phone: String,
    garment: String,
    labels: List<String>,
    paid: Double,
    remaining: Double
): File {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 25f; isFakeBoldText = true; color = android.graphics.Color.rgb(100, 36, 58) }
    val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 15f; isFakeBoldText = true; color = android.graphics.Color.rgb(100, 36, 58) }
    val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; color = android.graphics.Color.DKGRAY }
    val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(210, 163, 111); strokeWidth = 2f }
    canvas.drawText("NEW DUBAI", 42f, 62f, title)
    canvas.drawText("TAILOR & FABRIC STUDIO", 42f, 88f, body)
    canvas.drawLine(42f, 108f, 553f, 108f, gold)
    canvas.drawText("ORDER RECEIPT", 42f, 145f, heading)
    val rows = listOf(
        "${labels[0]}: ${order.no}",
        "${labels[1]}: $customer",
        "${labels[2]}: $phone",
        "${labels[3]}: $garment",
        "${labels[4]}: ${order.orderDate}",
        "${labels[5]}: ${order.deliveryDate}",
        "${labels[6]}: ${order.status}",
        "${labels[7]}: ${money(order.fabricPrice)}",
        "${labels[8]}: ${money(order.tailorPrice)}",
        "${labels[9]}: ${money(order.total)}",
        "${labels[10]}: ${money(paid)}",
        "${labels[11]}: ${money(remaining)}"
    )
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 17f
        color = android.graphics.Color.DKGRAY
    }
    val receiptText = rows.joinToString("\n\n")
    val layout = StaticLayout.Builder.obtain(receiptText, 0, receiptText.length, textPaint, 500)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
        .setLineSpacing(4f, 1.12f)
        .build()
    canvas.save()
    canvas.translate(48f, 178f)
    layout.draw(canvas)
    canvas.restore()
    val footerY = (178 + layout.height + 35).toFloat()
    canvas.drawLine(42f, footerY, 553f, footerY, gold)
    canvas.drawText("Thank you for choosing NEW DUBAI.", 42f, footerY + 34f, body)
    document.finishPage(page)
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "${order.no.replace(Regex("[^A-Za-z0-9_-]"), "_")}_receipt.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    return file
}

@Composable
fun ReceiptPdfAction(order: TailorOrder, customer: String, phone: String, paid: Double, remaining: Double) {
    val context = LocalContext.current
    val garment = garmentLabel(order.type)
    val labels = listOf(tr("order"), tr("customer"), tr("phone"), tr("type"), tr("odate"), tr("ddate"), tr("status"), tr("fprice"), tr("tprice"), tr("total"), tr("paid"), tr("rem"))
    Button(onClick = {
        runCatching {
            val file = createReceiptPdf(context, order, customer, phone, garment, labels, paid, remaining)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "NEW DUBAI receipt ${order.no}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share receipt PDF"))
        }.onFailure { Toast.makeText(context, it.message ?: "Could not create PDF", Toast.LENGTH_LONG).show() }
    }) { Text("PDF · Share receipt") }
}
