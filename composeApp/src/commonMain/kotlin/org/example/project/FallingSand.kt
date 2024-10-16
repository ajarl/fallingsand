package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import kotlin.math.roundToInt

const val WIDTH = 512
const val HEIGHT = 512

data class SandParticle(
    override var x: Int,
    override var y: Int,
    val color: Color,
    var restCount: Int = 0,
) : TwoDPoint

@Composable
private fun ColorButton(color: Color, name: String, selectedColor: Color, onSelect: (color: Color) -> Unit) {
    Button(onClick = { onSelect(color) }) {
        if (selectedColor == color) {
            Icon(Icons.Default.Check, "")
        }
        Text(name)
    }
}

@Composable
fun FallingSand() {
    val sandParticles = remember {
        Quadtree<SandParticle>(WIDTH, HEIGHT)
    }
    var drawState by remember { mutableStateOf(0L) }
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var fps by remember { mutableStateOf(99f) }

    LaunchedEffect(Unit) {
        var oldTime: Long? = null
        while (true) {
            withFrameMillis { time ->
                val it = sandParticles.activePoints.iterator()
                while (it.hasNext()) {
                    val particle = it.next()
                    val x = particle.x
                    val y = particle.y
                    if (particle.y < HEIGHT - 1) {
                        var updated = false
                        if (sandParticles.lookup(x, y + 1) == null) {
                            particle.y++
                            updated = true
                        } else {
                            if (sandParticles.lookup(x - 1, y + 1) == null) {
                                particle.x--
                                particle.y++
                                updated = true
                            } else if (sandParticles.lookup(x + 1, y + 1) == null) {
                                particle.x++
                                particle.y++
                                updated = true
                            }
                        }
                        if (updated) {
                            sandParticles.onPositionUpdated(particle, x, y)
                        } else {
                            particle.restCount++
                            if (particle.restCount >= 5) {
                                it.remove()
                                sandParticles.restedPoints.add(particle)
                            }
                        }
                    }
                }
                if (oldTime != null) {
                    val delta = time - oldTime!!
                    fps = fps * 0.9f + 0.1f * (1f / (delta / 1000f))
                    fps = (fps * 10).roundToInt() / 10f
                }

                drawState++
                oldTime = time
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text("FPS: $fps")

        Row {
            ColorButton(Color.White, "White", selectedColor, { selectedColor = it })
            ColorButton(Color.Red, "Red", selectedColor, { selectedColor = it })
            ColorButton(Color(0xFF1166FF), "Blue", selectedColor, { selectedColor = it })
            ColorButton(Color.Green, "Green", selectedColor, { selectedColor = it })
            ColorButton(Color.Yellow, "Yellow", selectedColor, { selectedColor = it })
        }

        val density = 3f
        Canvas(
            Modifier
                .width(with(LocalDensity.current) { (WIDTH * density).toDp() })
                .height(with(LocalDensity.current) { (HEIGHT * density).toDp() })
                .background(Color.Black)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: return@awaitPointerEventScope
                            if (!change.pressed) continue

                            val currentX = (change.position.x / density).toInt()
                            val currentY = (change.position.y / density).toInt()
                            val previousX = (change.previousPosition.x / density).toInt()
                            val previousY = (change.previousPosition.y / density).toInt()
                            val deltaX = currentX - previousX
                            val deltaY = currentY - previousY

                            for (xOffset in 0..abs(deltaX)) {
                                val x = previousX + ((xOffset / abs(deltaX).toFloat()) * deltaX).toInt()
                                val y = previousY + ((xOffset / abs(deltaX).toFloat()) * deltaY).toInt()

                                if (sandParticles.lookup(x, y) == null) {
                                    sandParticles.insert(SandParticle(x, y, selectedColor))
                                }
                            }
                            drawState++

                            change.consume()
                        }
                    }
                }
        ) {
            drawState

            fun drawParticle(particle: SandParticle) {
                drawRect(particle.color, Offset(particle.x * density, particle.y * density), size = Size(density, density))
            }

            for (particle in sandParticles.activePoints) {
                drawParticle(particle)
            }
            for (particle in sandParticles.restedPoints) {
                drawParticle(particle)
            }
        }
    }
}