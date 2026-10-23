package dev.codingcoffee.screen.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.codingcoffee.screen.app
import dev.codingcoffee.screen.data.LogEntry
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

    SectionHeader("Spam by time of day")
    ChartCard {
        StackedBars(
            bars = stats.byHour.map { it to 0 },
            labels = listOf(0 to "12am", 6 to "6am", 12 to "12pm", 18 to "6pm"),
        )
    }

    SectionHeader("Calls per day")
    ChartCard {
        StackedBars(bars = stats.days.map { it.blocked to it.allowed }, labels = dayLabels(stats.days))
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
        RateLine(stats.days, dayLabels(stats.days))
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
private fun StackedBars(bars: List<Pair<Int, Int>>, labels: List<Pair<Int, String>>) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val max = bars.maxOfOrNull { it.first + it.second }?.coerceAtLeast(1) ?: 1

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val plot = plotArea(measurer, labelStyle, "$max")
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
    }
}

@Composable
private fun RateLine(days: List<Day>, labels: List<Pair<Int, String>>) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val plot = plotArea(measurer, labelStyle, "100%")
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
private fun DrawScope.plotArea(measurer: TextMeasurer, style: TextStyle, widestYLabel: String): Plot {
    val yLabel = measurer.measure(widestYLabel, style).size
    val left = yLabel.width + 8.dp.toPx()
    return Plot(left, yLabel.height.toFloat(), size.width - left, size.height - yLabel.height - 4.dp.toPx())
}

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
