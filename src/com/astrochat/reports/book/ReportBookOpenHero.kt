package com.astrochat.reports.book

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Detail page hero book (Figma "Open book · Detail hero", 3785:11536; prototype R.6 / r2-bookwrap).
 *
 * Plays ONCE each time the detail page opens, then rests open:
 *  1. rises into place (0.12s → 0.62s): fade in, 14dp up, scale .97 → 1
 *  2. the cover swings over its spine (0.9s → 2.8s): 0° → -184° at 78% → settles at -180°,
 *     while the book slides right so the open spread lands on the centre line
 *  3. contact shadow + gutter fade in (2.5s → 3.0s)
 *  4. "See Sample report" pill fades in, straddling the foot of the book (2.6s → 2.95s)
 *
 * The spread then stays open — a book that keeps shutting itself on the page where she is
 * deciding to buy it withholds the thing she came to look at.
 * With animations off (system setting) or [animate] = false it renders the open end state.
 */
@Composable
fun ReportBookOpenHero(
    book: ReportBook,
    onSampleReportClick: () -> Unit,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    /** One page of the open spread. Figma: spread 183×122 → page 91.5×122. */
    pageWidth: Dp = 91.5.dp,
    pageHeight: Dp = 122.dp,
    sampleLabel: String = "See Sample report",
    pillFontFamily: FontFamily? = null,
) {
    val reduceMotion = rememberReduceMotion()
    val end = (BookTokens.PILL_DELAY_MS + BookTokens.PILL_MS).toFloat()
    val clock = remember(book) { Animatable(if (animate && !reduceMotion) 0f else end) }
    LaunchedEffect(book, animate, reduceMotion) {
        if (animate && !reduceMotion) {
            clock.snapTo(0f)
            clock.animateTo(end, tween(end.toInt(), easing = LinearEasing))
        } else clock.snapTo(end)
    }
    val spread = ImageBitmap.imageResource(book.spreadRes)

    fun phase(delay: Int, dur: Int): Float = ((clock.value - delay) / dur).coerceIn(0f, 1f)

    Box(
        modifier
            .size(pageWidth * 2, pageHeight + 18.dp)
            .semantics { contentDescription = book.contentDescription }
    ) {
        // ---- timeline ----
        val rise = BookTokens.RiseEasing.transform(phase(BookTokens.RISE_DELAY_MS, BookTokens.RISE_MS))
        val open = phase(BookTokens.OPEN_DELAY_MS, BookTokens.OPEN_MS)
        val coverAngle: Float
        val lay: Float
        if (open < 0.78f) {
            val p = BookTokens.TurnEasing.transform(open / 0.78f)
            coverAngle = -184f * p
            lay = p
        } else {
            val p = BookTokens.TurnEasing.transform((open - 0.78f) / 0.22f)
            coverAngle = -184f + 4f * p
            lay = 1f
        }
        val seat = phase(BookTokens.SEAT_DELAY_MS, BookTokens.SEAT_MS)
        val pill = phase(BookTokens.PILL_DELAY_MS, BookTokens.PILL_MS)

        // Everything lives in a spread-sized box (2 pages wide) that rises in.
        Box(
            Modifier
                .requiredSize(pageWidth * 2, pageHeight)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    alpha = rise
                    translationY = (1f - rise) * 14.dp.toPx()
                    val s = 0.97f + 0.03f * rise
                    scaleX = s; scaleY = s
                }
        ) {
            // Contact shadow: the book's weight on the hero.
            // Figma: ellipse 168×18 at (16.5, 122) on a 183×122 spread, so it sits *below* the
            // foot of the book and 9 to the right of centre — the light comes from the top left.
            Canvas(
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = pageWidth * 2 * (9f / 183f), y = 18.dp)
                    .requiredSize(pageWidth * 2 * (168f / 183f), 18.dp)
                    .alpha(seat)
            ) { drawEllipseShadow(BookTokens.umber(0.34f)) }

            // The book: spine at x=0 of this box. Shut, it is centred (starts half a page in);
            // as it opens it slides right by half a page so the gutter lands on the centre line.
            Box(
                Modifier
                    .requiredSize(pageWidth, pageHeight)
                    .align(Alignment.TopStart)
                    .graphicsLayer { translationX = (pageWidth * (0.5f + 0.5f * lay)).toPx() }
            ) {
                // Right-hand page, lying under the cover. The board passing overhead throws a
                // shadow across it, deepest when the cover is upright and gone once it is flat.
                Canvas(Modifier.fillMaxSize()) {
                    drawSpreadHalf(spread, right = true)
                    val shade = (lay * (1f - lay) * 4f).coerceIn(0f, 1f)
                    if (shade > 0.01f) {
                        drawRect(
                            Brush.horizontalGradient(
                                0.0f to BookTokens.umber(0.40f * shade),
                                1.0f to BookTokens.umber(0.06f * shade),
                            )
                        )
                    }
                }

                // Cover + first leaf, turning together on the spine.
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            rotationY = coverAngle
                            cameraDistance = 12f * density
                        }
                ) {
                    if (coverAngle > -90f) {
                        // Front: the painted cover.
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 5.dp, bottomEnd = 5.dp))
                        ) {
                            // The page is 3:4 but the cover art is 2:3, so Crop has to lose 12%
                            // of its height. Anchor it to the bottom and all of that comes off
                            // the top, where the art is only background — the title block at
                            // the foot of the cover stays whole.
                            Image(
                                painter = painterResource(book.coverRes),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.BottomCenter,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Canvas(Modifier.fillMaxSize()) { drawCoverLighting() }
                        }
                    } else {
                        // Back: the spread's left-hand page. Mirrored so it reads the right way
                        // round once the cover has swung past 90°.
                        Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = -1f }) {
                            drawSpreadHalf(spread, right = false)
                        }
                    }
                }
            }

            // Gutter: the two pages fall away into the spine, so the centre line is darkest.
            // Figma: rect 27×122 at x=78 on a 183×122 spread — full page height, centred.
            Canvas(
                Modifier
                    .align(Alignment.TopCenter)
                    .requiredSize(pageWidth * 2 * (27f / 183f), pageHeight)
                    .alpha(seat)
            ) {
                drawRect(
                    Brush.horizontalGradient(
                        0.00f to BookTokens.Gutter.copy(alpha = 0f),
                        0.44f to BookTokens.Gutter.copy(alpha = 0.30f),
                        0.50f to BookTokens.Gutter.copy(alpha = 0.36f),
                        0.56f to BookTokens.Gutter.copy(alpha = 0.30f),
                        1.00f to BookTokens.Gutter.copy(alpha = 0f),
                    ),
                    blendMode = BlendMode.Multiply,
                )
            }

            // "See Sample report", hanging off the foot of the open book, over the gutter.
            if (pill > 0f) {
                SampleReportPill(
                    label = sampleLabel,
                    fontFamily = pillFontFamily,
                    onClick = onSampleReportClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 4.dp)
                        .alpha(pill),
                )
            }
        }
    }
}

/** White pill: eye icon + "See Sample report" (Figma "Sample report pill", 153×30). */
@Composable
fun SampleReportPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "See Sample report",
    fontFamily: FontFamily? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier
            .height(30.dp)
            // 0 4 12 rgba(74,24,8,.22), 0 1 2 rgba(74,24,8,.14)
            .softShadow(BookTokens.umber(0.22f), blur = 12.dp, offsetY = 4.dp, cornerRadius = 15.dp)
            .softShadow(BookTokens.umber(0.14f), blur = 2.dp, offsetY = 1.dp, cornerRadius = 15.dp)
            .clip(shape)
            .background(if (pressed) BookTokens.PillPressed else Color.White, shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EyeIcon(Modifier.size(14.dp))
        Text(
            text = label,
            style = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = BookTokens.PillText,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun EyeIcon(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val eye = Path().apply {
            moveTo(w * 0.06f, h * 0.5f)
            quadraticBezierTo(w * 0.5f, h * 0.02f, w * 0.94f, h * 0.5f)
            quadraticBezierTo(w * 0.5f, h * 0.98f, w * 0.06f, h * 0.5f)
            close()
        }
        drawPath(eye, BookTokens.PillText, style = stroke)
        drawCircle(BookTokens.PillText, radius = w * 0.15f, center = Offset(w / 2, h / 2), style = stroke)
    }
}
