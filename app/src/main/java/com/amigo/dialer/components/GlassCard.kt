package com.amigo.dialer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    surfaceColor: Color = Color.White.copy(alpha = 0.08f),
    blurAmount: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val backdrop = remember {
        object : Backdrop {
            override val isCoordinatesDependent: Boolean = false
            override fun DrawScope.drawBackdrop(
                density: Density,
                coordinates: LayoutCoordinates?,
                layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)?
            ) {
                // Empty implementation
            }
        }
    }

    Column(
        modifier = modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { ContinuousRoundedRectangle(cornerRadius) },
            effects = {
                colorControls(
                    brightness = 0f,
                    saturation = 1.5f
                )
                blur(blurAmount.toPx())
                lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true)
            },
            onDrawSurface = {
                drawRect(surfaceColor)
            }
        ),
        content = content
    )
}
