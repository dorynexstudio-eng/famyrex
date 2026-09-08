package com.famyrex.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier

/**
 * Local fallback used only by the standalone status-pill composable, which is
 * intentionally not a RowScope composable. RowScope.weight remains preferred
 * whenever the caller is inside a Row.
 */
fun Modifier.weight(weight: Float): Modifier = fillMaxWidth((weight / 2f).coerceIn(0.1f, 1f))
