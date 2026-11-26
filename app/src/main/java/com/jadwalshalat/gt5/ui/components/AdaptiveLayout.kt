package com.jadwalshalat.gt5.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Container-based responsive breakpoints derived from BoxWithConstraints
 * (supports tablets, foldables, ChromeOS, and Android multi-window safely).
 */
enum class AdaptiveWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

object AdaptiveBreakpoints {
    val NavigationRailMinWidth: Dp = 600.dp
    val TwoColumnGridMinWidth: Dp = 680.dp
    val StudySplitMinWidth: Dp = 700.dp
    val StudySplitMinHeight: Dp = 360.dp
    val SingleColumnMaxWidth: Dp = 760.dp
    val WideCatalogMaxWidth: Dp = 1080.dp
    val FlashcardMaxWidth: Dp = 680.dp

    fun widthClass(maxWidth: Dp): AdaptiveWidthClass = when {
        maxWidth < 600.dp -> AdaptiveWidthClass.COMPACT
        maxWidth < 840.dp -> AdaptiveWidthClass.MEDIUM
        else -> AdaptiveWidthClass.EXPANDED
    }

    fun useNavigationRail(maxWidth: Dp): Boolean = maxWidth >= NavigationRailMinWidth

    fun useTwoColumnGrid(maxWidth: Dp): Boolean = maxWidth >= TwoColumnGridMinWidth

    fun useStudySplitPane(maxWidth: Dp, maxHeight: Dp): Boolean =
        maxWidth >= StudySplitMinWidth && maxHeight >= StudySplitMinHeight
}

/**
 * Centers content within a readable maximum width on wide screens while remaining
 * full-bleed on compact phones.
 */
@Composable
fun CenteredAdaptiveBox(
    maxContentWidth: Dp = AdaptiveBreakpoints.SingleColumnMaxWidth,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxContentWidth)
        ) {
            content()
        }
    }
}

/**
 * Renders 1 or 2 items in a balanced row for LazyColumn-based responsive grids.
 */
@Composable
fun <T> AdaptivePairRow(
    pair: List<T>,
    horizontalSpacing: Dp = 12.dp,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.weight(1f)) {
            itemContent(pair[0])
        }
        if (pair.size > 1) {
            Box(modifier = Modifier.weight(1f)) {
                itemContent(pair[1])
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
