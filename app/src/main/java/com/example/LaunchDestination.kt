package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private val BrandRed = Color(0xFFFF2B3D)

/** Original introductory content, with a reserved illustration panel on each page. */
@Composable
fun LaunchDestination(initiallyComplete: Boolean, onOnboardingComplete: () -> Unit) {
    var complete by rememberSaveable { mutableStateOf(initiallyComplete) }
    if (complete) { com.example.survey.SurveyScreen(); return }
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(260)) }
    val titles = intArrayOf(R.string.intro_title_1, R.string.intro_title_2, R.string.intro_title_3)
    val descriptions = intArrayOf(R.string.intro_body_1, R.string.intro_body_2, R.string.intro_body_3)

    Box(Modifier.fillMaxSize().background(Color(0xFF292D32)).safeDrawingPadding()) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp)) {
            // Compact windows can scroll; ordinary phones give all remaining height to the panel.
            val contentHeight = maxHeight.coerceAtLeast(560.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth().height(contentHeight).graphicsLayer { alpha = entrance.value }) {
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(color = BrandRed)) { append("bye") }
                        withStyle(SpanStyle(color = Color.White)) { append("smo") }
                    }, modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(24.dp))
                    if (complete) {
                        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                            Text(stringResource(R.string.welcome_title), color = Color.White,
                                style = MaterialTheme.typography.headlineLarge)
                            Spacer(Modifier.height(20.dp))
                            Text(stringResource(R.string.welcome_body), color = Color(0xFFD1D3D6),
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(),
                            pageSpacing = 24.dp) { page ->
                            Column(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f).fillMaxWidth().padding(6.dp)
                                    .drawBehind {
                                        // Inset layers keep the shadow under the panel, with a soft lower edge.
                                        repeat(16) { layer ->
                                            val inset = (2f + layer * 0.55f).dp.toPx()
                                            val drop = (10f - layer * 0.4f).dp.toPx()
                                            drawRoundRect(Color.Black.copy(alpha = 0.012f),
                                                topLeft = Offset(inset, drop),
                                                size = Size((size.width - inset * 2).coerceAtLeast(0f), size.height),
                                                cornerRadius = CornerRadius(24.dp.toPx()))
                                        }
                                    }
                                    .background(Color(0xFF373F47), RoundedCornerShape(24.dp)))
                                Spacer(Modifier.height(24.dp))
                                Text(stringResource(titles[page]), modifier = Modifier.padding(horizontal = 6.dp), color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium)
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(descriptions[page]), modifier = Modifier.padding(horizontal = 6.dp), color = Color(0xFFD1D3D6),
                                    style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        FlowingDots(pager.currentPage + pager.currentPageOffsetFraction,
                            pager.settledPage, Modifier.align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(24.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                            contentPadding = PaddingValues(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = Color.White),
                            onClick = {
                                if (!pager.isScrollInProgress) {
                                    if (pager.currentPage < 2) scope.launch {
                                        pager.animateScrollToPage(pager.currentPage + 1,
                                            animationSpec = tween(380, easing = FastOutSlowInEasing))
                                    } else {
                                        onOnboardingComplete()
                                        complete = true
                                    }
                                }
                            },
                        ) { Text(stringResource(if (pager.currentPage < 2) R.string.intro_next else R.string.intro_start),
                            fontSize = MaterialTheme.typography.labelLarge.fontSize.value.plus(1f).sp) }
                    }
                }
            }
        }
    }
}

/** Stretch the leading edge first, then let the trailing edge catch up. Works in both directions. */
@Composable
private fun FlowingDots(position: Float, settledPage: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.intro_progress, settledPage + 1, 3)
    Canvas(modifier.size(width = 72.dp, height = 12.dp).semantics { contentDescription = description }) {
        val radius = 4.dp.toPx()
        val step = 24.dp.toPx()
        val first = size.width / 2f - step
        val centerY = size.height / 2f
        repeat(3) { drawCircle(Color(0xFF626970), radius, Offset(first + it * step, centerY)) }
        val clamped = position.coerceIn(0f, 2f)
        val base = floor(clamped)
        val fraction = clamped - base
        val left = first + (base + max(0f, fraction * 2f - 1f)) * step - radius
        val right = first + (base + min(1f, fraction * 2f)) * step + radius
        drawRoundRect(BrandRed, Offset(left, centerY - radius), Size(right - left, radius * 2),
            CornerRadius(radius, radius))
    }
}
