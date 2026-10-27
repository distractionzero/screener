package dev.codingcoffee.screener.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.codingcoffee.screener.app
import dev.codingcoffee.screener.data.LogEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

private enum class Range(val label: String, val days: Long?) { WEEK("7 days", 7), MONTH("30 days", 30), ALL("All", null) }

private data class Day(val date: LocalDate, val blocked: Int, val allowed: Int) {
    val total get() = blocked + allowed
}

private data class Headline(val today: Int, val week: Int, val total: Int, val maxDay: Int)

private data class Stats(
    val byHour: List<Int>,
    val days: List<Day>,
    val byReason: List<Pair<String, Int>>,
    val blocked: Int,
    val total: Int,
)

private class Dated(val entry: LogEntry, val date: LocalDate, val hour: Int)

private val ColorScheme.blocked get() = lerp(errorContainer, error, 0.3f)
private val ColorScheme.allowed get() = lerp(primaryContainer, primary, 0.3f)

private val DayLabel = DateTimeFormatter.ofPattern("d MMM")

private enum class Chart { HOUR, DAY, RATE }

private class Tooltip(val title: String, val lines: List<Pair<Color, String>>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    val app = LocalContext.current.app
    val entries by app.callLog.entries.collectAsStateWithLifecycle()
    var range by rememberSaveable { mutableStateOf(Range.WEEK) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val today = LocalDate.now()
    val dated = remember(entries) {
        val zone = ZoneId.systemDefault()
        entries?.filterNot { it.outgoing }?.map {
            val time = Instant.ofEpochMilli(it.timestamp).atZone(zone)
            Dated(it, time.toLocalDate(), time.hour)
        }
    }
    val headline = remember(dated, today) { dated?.let { headline(it, today) } }
    val stats = remember(dated, range, today) { dated?.let { stats(it, range, today) } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("Stats") }, scrollBehavior = scrollBehavior) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            when {
                dated == null || headline == null || stats == null -> Unit
                dated.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.BarChart,
                    title = "No stats yet",
                    body = "Stats will show up once calls are screened.",
                )
                else -> {
                    HeadlineTiles(headline)
                    SingleChoiceSegmentedButtonRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    ) {
                        Range.entries.forEachIndexed { i, r ->
                            SegmentedButton(
                                selected = range == r,
                                onClick = { range = r },
                                shape = SegmentedButtonDefaults.itemShape(i, Range.entries.size),
                            ) { Text(r.label) }
                        }
                    }
                    StatsCharts(stats)
                }
            }
        }
    }
}

@Composable
private fun HeadlineTiles(headline: Headline) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("Blocked today", headline.today, Modifier.weight(1f))
            Tile("Blocked this week", headline.week, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("Total blocked", headline.total, Modifier.weight(1f))
            Tile("Most in a day", headline.maxDay, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Tile(label: String, value: Int, modifier: Modifier) {
    TonalCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text("$value", style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatsCharts(stats: Stats) {
    val colors = MaterialTheme.colorScheme
    var selected by remember(stats) { mutableStateOf<Pair<Chart, Int>?>(null) }
    val selectedIn = { chart: Chart -> selected?.takeIf { it.first == chart }?.second }
    val select = { chart: Chart, index: Int? ->
        selected = index?.let { chart to it }.takeIf { it != selected }
    }

    SectionHeader("Spam by time of day")
    ChartCard {
        StackedBars(
            bars = stats.byHour.map { it to 0 },
            labels = listOf(0, 6, 12, 18).map { it to hourLabel(it) },
            selected = selectedIn(Chart.HOUR),
            onSelect = { select(Chart.HOUR, it) },
            tooltip = {
                Tooltip("${hourLabel(it)} – ${hourLabel((it + 1) % 24)}", listOf(colors.blocked to "${stats.byHour[it]} blocked"))
            },
        )
    }

    SectionHeader("Calls per day")
    ChartCard {
        StackedBars(
            bars = stats.days.map { it.blocked to it.allowed },
            labels = dayLabels(stats.days),
            selected = selectedIn(Chart.DAY),
            onSelect = { select(Chart.DAY, it) },
            tooltip = {
                val day = stats.days[it]
                Tooltip(
                    day.date.format(DayLabel),
                    listOf(colors.blocked to "${day.blocked} blocked", colors.allowed to "${day.allowed} allowed"),
                )
            },
        )
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend("Blocked", colors.blocked)
            Legend("Allowed", colors.allowed)
        }
    }

    SectionHeader("Block rate")
    ChartCard {
        val rate = if (stats.total == 0) 0 else (stats.blocked * 100f / stats.total).roundToInt()
        Text("$rate%", style = MaterialTheme.typography.headlineMedium)
        Text(
            "${stats.blocked} of ${stats.total} calls blocked",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        RateLine(stats.days, dayLabels(stats.days), selectedIn(Chart.RATE)) { select(Chart.RATE, it) }
    }

    SectionHeader("Blocks by rule")
    ChartCard {
        if (stats.byReason.isEmpty()) {
            Text("No blocked calls in this period", color = colors.onSurfaceVariant)
        } else {
            ReasonBars(stats.byReason)
        }
    }
}

@Composable
private fun ChartCard(content: @Composable () -> Unit) {
    TonalCard {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StackedBars(
    bars: List<Pair<Int, Int>>,
    labels: List<Pair<Int, String>>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    tooltip: (Int) -> Tooltip,
) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val max = bars.maxOfOrNull { it.first + it.second }?.coerceAtLeast(1) ?: 1
    val select by rememberUpdatedState(onSelect)
    val drawTooltip = tooltipDrawer(measurer)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(bars) {
                detectTapGestures { tap ->
                    val plot = plotArea(measurer, labelStyle, "$max", size.toSize())
                    select(slotAt(tap.x, plot, bars.size))
                }
            }
    ) {
        val plot = plotArea(measurer, labelStyle, "$max", size)
        gridLine(plot, plot.top, colors.outlineVariant)
        gridLine(plot, plot.bottom, colors.outlineVariant)
        yLabel(measurer, labelStyle, "$max", plot.top)

        val slot = plot.width / bars.size
        val barWidth = (slot * 0.7f).coerceIn(1f, 24.dp.toPx())
        val radius = CornerRadius((barWidth / 4).coerceAtMost(4.dp.toPx()))
        bars.forEachIndexed { i, (blocked, allowed) ->
            val x = plot.left + i * slot + (slot - barWidth) / 2
            val blockedHeight = plot.height * blocked / max
            val allowedHeight = plot.height * allowed / max
            if (blocked > 0) {
                drawRoundRect(colors.blocked, Offset(x, plot.bottom - blockedHeight), Size(barWidth, blockedHeight), radius)
            }
            if (allowed > 0) {
                val top = plot.bottom - blockedHeight - allowedHeight
                drawRoundRect(colors.allowed, Offset(x, top), Size(barWidth, allowedHeight), radius)
            }
        }
        xLabels(measurer, labelStyle, labels, plot, slot)
        selected?.let { i ->
            val (blocked, allowed) = bars[i]
            val top = plot.bottom - plot.height * (blocked + allowed) / max
            drawTooltip(tooltip(i), Offset(plot.left + i * slot + slot / 2, top))
        }
    }
}

@Composable
private fun RateLine(days: List<Day>, labels: List<Pair<Int, String>>, selected: Int?, onSelect: (Int?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val select by rememberUpdatedState(onSelect)
    val drawTooltip = tooltipDrawer(measurer)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .pointerInput(days) {
                detectTapGestures { tap ->
                    val plot = plotArea(measurer, labelStyle, "100%", size.toSize())
                    select(slotAt(tap.x, plot, days.size)?.takeIf { days[it].total > 0 })
                }
            }
    ) {
        val plot = plotArea(measurer, labelStyle, "100%", size)
        listOf(1f to "100%", 0.5f to "50%", 0f to "0%").forEach { (fraction, text) ->
            val y = plot.bottom - plot.height * fraction
            gridLine(plot, y, colors.outlineVariant)
            yLabel(measurer, labelStyle, text, y)
        }

        val slot = plot.width / days.size
        val points = days.mapIndexed { i, day ->
            day.takeIf { it.total > 0 }?.let {
                Offset(plot.left + i * slot + slot / 2, plot.bottom - plot.height * it.blocked / it.total)
            }
        }
        val path = Path()
        points.forEachIndexed { i, point ->
            if (point == null) return@forEachIndexed
            if (points.getOrNull(i - 1) == null) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, colors.blocked, style = Stroke(2.dp.toPx()))
        if (days.size <= 31) points.filterNotNull().forEach { drawCircle(colors.blocked, 3.dp.toPx(), it) }
        xLabels(measurer, labelStyle, labels, plot, slot)
        selected?.let { i ->
            val point = points[i] ?: return@let
            val rate = (days[i].blocked * 100f / days[i].total).roundToInt()
            drawCircle(colors.blocked, 5.dp.toPx(), point)
            drawTooltip(Tooltip(days[i].date.format(DayLabel), listOf(colors.blocked to "$rate% blocked")), point)
        }
    }
}

@Composable
private fun ReasonBars(reasons: List<Pair<String, Int>>) {
    val max = reasons.first().second
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        reasons.forEach { (reason, count) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row {
                    Text(reason, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("$count", style = MaterialTheme.typography.bodyMedium)
                }
                Box(
                    Modifier
                        .fillMaxWidth(count.toFloat() / max)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.blocked, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

private class Plot(val left: Float, val top: Float, val width: Float, val bottom: Float) {
    val height get() = bottom - top
}

/** Leaves room for y labels on the left and x labels below. */
private fun Density.plotArea(measurer: TextMeasurer, style: TextStyle, widestYLabel: String, size: Size): Plot {
    val yLabel = measurer.measure(widestYLabel, style).size
    val left = yLabel.width + 8.dp.toPx()
    return Plot(left, yLabel.height.toFloat(), size.width - left, size.height - yLabel.height - 4.dp.toPx())
}

private fun slotAt(x: Float, plot: Plot, count: Int): Int? {
    if (count == 0 || x < plot.left) return null
    return ((x - plot.left) / (plot.width / count)).toInt().takeIf { it < count }
}

/** Draws above the anchor, clamped to the canvas. */
@Composable
private fun tooltipDrawer(measurer: TextMeasurer): DrawScope.(Tooltip, Offset) -> Unit {
    val colors = MaterialTheme.colorScheme
    val titleStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val lineStyle = MaterialTheme.typography.labelLarge.copy(color = colors.onSurface)
    return { tooltip, anchor ->
        val title = measurer.measure(tooltip.title, titleStyle)
        val lines = tooltip.lines.map { (color, text) -> color to measurer.measure(text, lineStyle) }
        val pad = 10.dp.toPx()
        val gap = 2.dp.toPx()
        val dot = 8.dp.toPx()
        val dotGap = 6.dp.toPx()
        val contentWidth = maxOf(title.size.width.toFloat(), lines.maxOf { dot + dotGap + it.second.size.width })
        val contentHeight = title.size.height + lines.sumOf { it.second.size.height } + gap * lines.size
        val box = Size(contentWidth + pad * 2, contentHeight + pad * 2)
        val topLeft = Offset(
            (anchor.x - box.width / 2).coerceIn(0f, (size.width - box.width).coerceAtLeast(0f)),
            (anchor.y - box.height - 8.dp.toPx()).coerceAtLeast(0f),
        )
        val radius = CornerRadius(8.dp.toPx())
        drawRoundRect(colors.surfaceContainerLowest, topLeft, box, radius)
        drawRoundRect(colors.outlineVariant, topLeft, box, radius, style = Stroke(1.dp.toPx()))
        drawText(title, topLeft = topLeft + Offset(pad, pad))
        var y = topLeft.y + pad + title.size.height + gap
        lines.forEach { (color, layout) ->
            drawCircle(color, dot / 2, Offset(topLeft.x + pad + dot / 2, y + layout.size.height / 2))
            drawText(layout, topLeft = Offset(topLeft.x + pad + dot + dotGap, y))
            y += layout.size.height + gap
        }
    }
}

private fun hourLabel(hour: Int) = "${(hour + 11) % 12 + 1}${if (hour < 12) "am" else "pm"}"

private fun DrawScope.gridLine(plot: Plot, y: Float, color: Color) {
    drawLine(color, Offset(plot.left, y), Offset(size.width, y), 1.dp.toPx())
}

private fun DrawScope.yLabel(measurer: TextMeasurer, style: TextStyle, text: String, y: Float) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(0f, y - layout.size.height / 2))
}

private fun DrawScope.xLabels(
    measurer: TextMeasurer,
    style: TextStyle,
    labels: List<Pair<Int, String>>,
    plot: Plot,
    slot: Float,
) {
    labels.forEach { (index, text) ->
        val layout = measurer.measure(text, style)
        val center = plot.left + index * slot + slot / 2
        val x = (center - layout.size.width / 2).coerceIn(plot.left, size.width - layout.size.width)
        drawText(layout, topLeft = Offset(x, plot.bottom + 4.dp.toPx()))
    }
}

private fun dayLabels(days: List<Day>): List<Pair<Int, String>> {
    if (days.isEmpty()) return emptyList()
    val last = days.lastIndex
    return listOf(0, last / 2, last).distinct().map { it to days[it].date.format(DayLabel) }
}

private fun headline(entries: List<Dated>, today: LocalDate): Headline {
    val blocked = entries.filter { it.entry.blocked }
    val weekStart = today.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek))
    return Headline(
        today = blocked.count { it.date == today },
        week = blocked.count { !it.date.isBefore(weekStart) },
        total = blocked.size,
        maxDay = blocked.groupingBy { it.date }.eachCount().values.maxOrNull() ?: 0,
    )
}

private fun stats(entries: List<Dated>, range: Range, today: LocalDate): Stats {
    val start = range.days?.let { today.minusDays(it - 1) } ?: entries.minOfOrNull { it.date } ?: today
    val inRange = entries.filter { !it.date.isBefore(start) }
    val blocked = inRange.filter { it.entry.blocked }
    val byDate = inRange.groupBy { it.date }
    val days = generateSequence(start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(today) }
        .map { date ->
            val calls = byDate[date].orEmpty()
            Day(date, calls.count { it.entry.blocked }, calls.count { !it.entry.blocked })
        }
        .toList()
    return Stats(
        byHour = List(24) { hour -> blocked.count { it.hour == hour } },
        days = days,
        byReason = blocked.groupingBy { it.entry.reason.replaceFirstChar(Char::uppercase) }.eachCount()
            .toList()
            .sortedByDescending { it.second },
        blocked = blocked.size,
        total = inRange.size,
    )
}
