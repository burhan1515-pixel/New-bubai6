@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.newdubai.shop.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.newdubai.shop.data.Calc
import com.newdubai.shop.util.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

fun money(v: Double): String {
    val r = Calc.round2(v)
    val s = if (r == Math.floor(r)) String.format(Locale.US, "%,.0f", r) else String.format(Locale.US, "%,.2f", r)
    return "$s AFN"
}

fun Modifier.pressable(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.55f), label = "press")
    this.scale(sc).clickable(interactionSource = src, indication = null, onClick = onClick)
}

@Composable
fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 70L); shown = true }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(420)) + slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it / 3 }
    ) { content() }
}

@Composable
fun CountUp(value: Double, style: TextStyle, color: Color = Color.Unspecified, fmt: (Double) -> String = { money(it) }) {
    val a by animateFloatAsState(value.toFloat(), tween(1000, easing = FastOutSlowInEasing), label = "count")
    Text(fmt(a.toDouble()), style = style, color = color)
}

/** Animated atelier emblem with a moving tailoring shear and stitch line. */
@Composable
fun AtelierNeedleMark(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "atelierNeedle")
    val angle by transition.animateFloat(
        initialValue = -16f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(720, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "needleAngle"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bobbinPulse"
    )
    Box(
        modifier.size(76.dp).clip(RoundedCornerShape(25.dp))
            .background(Brush.linearGradient(listOf(Lapis, LapisDeep, Color(0xFF9A6545))))
            .border(1.dp, Saffron.copy(alpha = .65f), RoundedCornerShape(25.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.ContentCut, contentDescription = null,
            modifier = Modifier.graphicsLayer { rotationZ = angle; scaleX = pulse; scaleY = pulse }.size(39.dp),
            tint = Champagne
        )
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).width(34.dp).height(2.dp)
            .clip(CircleShape).background(Saffron))
    }
}

@Composable
fun AuroraBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val inf = rememberInfiniteTransition(label = "aurora")
    val o by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), label = "o"
    )
    val brush = Brush.linearGradient(
        colors = listOf(LapisDeep, Color(0xFF492333), Lapis, Color(0xFF9A6545)),
        start = Offset(0f, 0f), end = Offset(500f + 900f * o, 400f + 300f * o)
    )
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier.shadow(12.dp, shape).clip(shape).background(brush)
            .border(1.dp, Saffron.copy(alpha = .48f), shape),
        content = content
    )
}

/** Animated stitched thread: dashes travel along a wavy line. */
@Composable
fun StitchThread(modifier: Modifier = Modifier, color: Color = Saffron) {
    val inf = rememberInfiniteTransition(label = "thread")
    val phase by inf.animateFloat(
        initialValue = 0f, targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "phase"
    )
    Canvas(modifier) {
        val w = size.width; val h = size.height; val seg = w / 8f
        val path = Path().apply {
            moveTo(0f, h / 2f)
            var x = 0f; var up = true
            while (x < w) {
                cubicTo(x + seg / 3f, if (up) 0f else h, x + 2f * seg / 3f, if (up) 0f else h, x + seg, h / 2f)
                x += seg; up = !up
            }
        }
        drawPath(path, color, style = Stroke(width = 4f, cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 12f), phase)))
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(23.dp)
    val m = modifier
        .animateContentSize(tween(260, easing = FastOutSlowInEasing))
        .let { if (onClick != null) it.pressable(onClick) else it }
        .shadow(if (onClick != null) 8.dp else 3.dp, shape)
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(cs.surface, cs.surface.copy(alpha = .98f), cs.surfaceVariant.copy(alpha = .34f))
            )
        )
        .border(1.dp, Brush.linearGradient(listOf(cs.primary.copy(alpha = .24f), cs.outline.copy(alpha = .42f))), shape)
    Column(m.padding(17.dp), content = content)
}

@Composable
fun Avatar(name: String) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(48.dp).shadow(5.dp, CircleShape).clip(CircleShape)
            .background(Brush.linearGradient(listOf(cs.primary, cs.secondary, Saffron.copy(alpha = .88f)))),
        contentAlignment = Alignment.Center
    ) { Text(name.trim().take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.titleMedium) }
}

@Composable
fun StatusChip(status: String) {
    val cs = MaterialTheme.colorScheme
    val c = when (status) {
        "new" -> cs.secondary; "wait" -> Color(0xFFE8A33D); "prog" -> cs.primary
        "done" -> cs.tertiary; else -> cs.outline
    }
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = .14f))
            .border(1.dp, c.copy(alpha = .28f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(c))
        Text(tr(status), color = c, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun Empty() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(tr("nodata"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionTitle(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(Modifier.width(4.dp).height(21.dp).clip(RoundedCornerShape(50)).background(Saffron))
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun KV(k: String, v: String, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(v, color = color, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun TField(
    label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth(),
    kb: KeyboardType = KeyboardType.Text, lines: Int = 1
) {
    val bringRequester = remember { BringIntoViewRequester() }
    val fieldScope = rememberCoroutineScope()
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier.padding(vertical = 4.dp)
            .bringIntoViewRequester(bringRequester)
            .onFocusChanged { focus ->
                if (focus.isFocused) fieldScope.launch {
                    delay(180)
                    runCatching { bringRequester.bringIntoView() }
                }
            },
        keyboardOptions = KeyboardOptions(keyboardType = kb), singleLine = lines == 1, minLines = lines,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = .75f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .22f)
        )
    )
}

@Composable
fun Select(label: String, options: List<Pair<String, String>>, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.padding(vertical = 3.dp)) {
        OutlinedTextField(
            value = options.find { it.first == value }?.second ?: "", onValueChange = {}, readOnly = true,
            label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.primary) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = .75f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .22f)
            )
        )
        Box(Modifier.matchParentSize().clickable { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.heightIn(max = 460.dp)
        ) {
            options.forEach { o -> DropdownMenuItem(text = { Text(o.second) }, onClick = { onChange(o.first); open = false }) }
        }
    }
}


/** Country/region filter makes the full traditional clothing catalogue easy to browse. */
@Composable
fun GarmentTypeSelect(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    var group by remember { mutableStateOf(Calc.garmentGroup(value)) }
    val groups = Calc.GARMENT_GROUPS
    val types = (Calc.MEN + Calc.KIDS).filter { Calc.garmentGroup(it) == group }
    Column(modifier) {
        Text(tr("country_style"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(groups) { key ->
                FilterChip(selected = group == key, onClick = {
                    group = key
                    val first = (Calc.MEN + Calc.KIDS).firstOrNull { Calc.garmentGroup(it) == key }
                    if (first != null && Calc.garmentGroup(value) != key) onChange(first)
                }, label = { Text(tr(key)) })
            }
        }
        // Free text is intentional: shops can enter garment names not in the preset catalogue.
        val customPrefix = if (group == "general") "m_custom:" else "m_custom:${group}:"
        val visibleValue = when {
            value.startsWith("m_custom:") -> value.substringAfter(':').substringAfter(':')
            value.startsWith("c_custom:") -> value.substringAfter(':').substringAfter(':')
            value in (Calc.MEN + Calc.KIDS) -> tr(value)
            else -> value
        }
        TField(label, visibleValue, { typed ->
            val prefix = if (value.startsWith("c_") || value.startsWith("c_custom:")) "c_custom:" else customPrefix
            onChange(prefix + typed.trimStart())
        })
        Text("${tr("type")} · ${tr("country_style")}", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(types) { item ->
                FilterChip(selected = value == item, onClick = { onChange(item) }, label = { Text(tr(item)) })
            }
        }
    }
}


fun garmentName(type: String): String = when {
    type.startsWith("m_custom:") || type.startsWith("c_custom:") -> type.substringAfterLast(':')
    else -> type
}

@Composable
fun garmentLabel(type: String): String = if (type.startsWith("m_custom:") || type.startsWith("c_custom:"))
    garmentName(type) else tr(type)

@Composable
fun DateField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.padding(vertical = 3.dp)) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
            modifier = Modifier.fillMaxWidth(), trailingIcon = { Icon(Icons.Default.DateRange, null) }
        )
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val init = runCatching { LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
        val st = rememberDatePickerState(initialSelectedDateMillis = init)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    open = false
                }) { Text(tr("ok")) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(tr("cancel")) } }
        ) { DatePicker(state = st) }
    }
}

@Composable
fun Sheet(title: String, onDismiss: () -> Unit, actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    val sc = remember { Animatable(0.88f) }
    LaunchedEffect(Unit) { sc.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 380f)) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(0.94f).heightIn(max = 700.dp).imePadding().graphicsLayer { scaleX = sc.value; scaleY = sc.value; alpha = sc.value.coerceIn(0f, 1f) },
            shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ContentCut, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Saffron.copy(alpha = .8f), MaterialTheme.colorScheme.outline.copy(alpha = .2f)))))
                Spacer(Modifier.height(10.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), content = content)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), content = actions)
            }
        }
    }
}

@Composable
fun ConfirmDialog(onYes: () -> Unit, onDismiss: () -> Unit, text: String = tr("sure")) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(text) },
        confirmButton = { Button(onClick = { onYes(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(tr("del")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("cancel")) } }
    )
}

@Composable
fun ErrText(err: String?) {
    if (err != null) Text(tr(err), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
}

@Composable
fun BarChart(rows: List<Triple<String, Double, Double>>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val p = remember(rows) { Animatable(0f) }
    LaunchedEffect(rows) { p.snapTo(0f); p.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            if (rows.isEmpty()) return@Canvas
            val mx = maxOf(1.0, rows.maxOf { maxOf(it.second, it.third) }).toFloat()
            val slot = size.width / rows.size; val bw = slot * 0.34f
            drawLine(cs.outline.copy(alpha = .5f), Offset(0f, size.height), Offset(size.width, size.height), 2f)
            rows.forEachIndexed { i, r ->
                val x = i * slot + slot * 0.12f
                val h1 = r.second.toFloat() / mx * size.height * p.value
                val h2 = r.third.toFloat() / mx * size.height * p.value
                drawRoundRect(cs.primary, Offset(x, size.height - h1), Size(bw, h1), CornerRadius(8f, 8f))
                drawRoundRect(cs.secondary, Offset(x + bw + 2f, size.height - h2), Size(bw, h2), CornerRadius(8f, 8f))
            }
        }
        if (rows.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(rows.first().first, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                if (rows.size > 2) Text(rows[rows.size / 2].first, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Text(rows.last().first, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun Donut(slices: List<Pair<Double, Color>>, modifier: Modifier = Modifier.size(130.dp)) {
    val p = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) { p.snapTo(0f); p.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    val track = MaterialTheme.colorScheme.outline.copy(alpha = .3f)
    Canvas(modifier) {
        val tot = slices.sumOf { it.first }
        val sw = size.minDimension * 0.18f
        val tl = Offset(sw / 2f, sw / 2f); val sz = Size(size.width - sw, size.height - sw)
        if (tot <= 0.0) { drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(sw)); return@Canvas }
        var a = -90f
        slices.forEach { (v, c) ->
            val sweep = (v / tot * 360.0).toFloat() * p.value
            if (sweep > 1f) drawArc(c, a, sweep - 1f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Butt))
            a += sweep
        }
    }
}

fun decodePhoto(b64: String): ImageBitmap? = runCatching {
    val b = Base64.decode(b64, Base64.NO_WRAP)
    BitmapFactory.decodeByteArray(b, 0, b.size).asImageBitmap()
}.getOrNull()

/** Reads an image Uri, scales it down to ~400px and returns JPEG as base64 text. */
fun encodePhoto(ctx: Context, uri: Uri): String? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 800) sample *= 2
    val bmp = ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return@runCatching null
    val s = 400f / maxOf(bmp.width, bmp.height)
    val scaled = if (s < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * s).toInt(), (bmp.height * s).toInt(), true) else bmp
    val bo = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 78, bo)
    Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP)
}.getOrNull()

@Composable
fun ListScreen(
    query: String, onQuery: (String) -> Unit, onAdd: (() -> Unit)?,
    header: (@Composable () -> Unit)? = null, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query, onValueChange = onQuery, singleLine = true, shape = RoundedCornerShape(50),
                leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text(tr("search")) },
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp)
            )
            header?.invoke()
            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp), content = content
            )
        }
        if (onAdd != null) {
            FloatingActionButton(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, null) }
        }
    }
}
