package com.github.manager.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val PullToRefreshThreshold = 80.dp
private val MaxPullDistance = 160.dp

class CustomPullToRefreshState {
    var isRefreshing by mutableStateOf(false)
        internal set
    var progress by mutableStateOf(0f)
        internal set
    var pullOffset by mutableStateOf(0f)
        internal set

    private var thresholdPx = 0f
    private var maxPullPx = 0f

    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (source != NestedScrollSource.Drag) return Offset.Zero
            if (isRefreshing) return Offset.Zero
            if (available.y > 0f) {
                val newOffset = (pullOffset + available.y).coerceAtMost(maxPullPx)
                val consumedY = newOffset - pullOffset
                pullOffset = newOffset
                progress = (pullOffset / thresholdPx).coerceIn(0f, 1f)
                return Offset(0f, consumedY)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Offset {
            if (pullOffset >= thresholdPx && !isRefreshing) {
                isRefreshing = true
            }
            return Offset.Zero
        }
    }

    internal fun updateThresholds(thresholdPx: Float, maxPullPx: Float) {
        this.thresholdPx = thresholdPx
        this.maxPullPx = maxPullPx
    }

    fun endRefresh() {
        isRefreshing = false
    }
}

@Composable
fun rememberCustomPullToRefreshState(): CustomPullToRefreshState {
    return remember { CustomPullToRefreshState() }
}

@Composable
fun PullToRefreshBox(
    state: CustomPullToRefreshState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { PullToRefreshThreshold.toPx() }
    val maxPullPx = with(density) { MaxPullDistance.toPx() }

    LaunchedEffect(thresholdPx, maxPullPx) {
        state.updateThresholds(thresholdPx, maxPullPx)
    }

    var triggered by remember { mutableStateOf(false) }

    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing && !triggered) {
            triggered = true
            onRefresh()
        }
        if (!state.isRefreshing) {
            triggered = false
            animate(
                initialValue = state.pullOffset,
                targetValue = 0f,
                animationSpec = spring()
            ) { value, _ ->
                state.pullOffset = value
                state.progress = (value / thresholdPx).coerceIn(0f, 1f)
            }
        }
    }

    Box(modifier = modifier.nestedScroll(state.nestedScrollConnection)) {
        content()

        if (state.pullOffset > 0.5f || state.isRefreshing) {
            val indicatorOffset = if (state.isRefreshing) {
                with(density) { PullToRefreshThreshold.toPx() }
            } else {
                state.pullOffset
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        with(density) { (indicatorOffset / density.density).dp }
                            .coerceAtMost(MaxPullDistance)
                    )
                    .offset {
                        IntOffset(
                            0,
                            -with(density) { PullToRefreshThreshold.toPx().roundToInt() }
                                    + indicatorOffset.roundToInt()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.height(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}
