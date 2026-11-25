package com.astrochat.reports.book

import android.graphics.BlurMaskFilter
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * A real blurred shadow (Android BlurMaskFilter), offset and spread like a CSS box-shadow.
 * Used instead of Modifier.shadow because the prototype's shadows are tinted umber, not grey,
 * and some are offset sideways (the cover's shadow falls onto the open page).
 */
internal fun Modifier.softShadow(
    color: Color,
    blur: Dp,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    spread: Dp = 0.dp,
    cornerRadius: Dp = 0.dp,
): Modifier = drawBehind {
    val paint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        if (blur > 0.dp) maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
    }
    val s = spread.toPx()
    val r = cornerRadius.toPx()
    drawIntoCanvas {
        it.nativeCanvas.drawRoundRect(
            -s + offsetX.toPx(), -s + offsetY.toPx(),
            size.width + s + offsetX.toPx(), size.height + s + offsetY.toPx(),
            r, r, paint,
        )
    }
}

/**
 * The shadow the front board throws onto the page it is lifting off. A board has weight: shut it
 * is a tight dark line along the fore-edge, ajar it swings wide, soft and further onto the page.
 *
 * [angle] is read in the draw phase (0 shut → [BookTokens.FLUTTER_OPEN_DEG] fully ajar), so the
 * shadow tracks the flutter without recomposing.
 */
internal fun Modifier.coverLiftShadow(angle: () -> Float): Modifier = drawBehind {
    val t = (angle() / BookTokens.FLUTTER_OPEN_DEG).coerceIn(0f, 1f)
    val paint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        // Kept light once the board is ajar: a heavy shadow here lands exactly on the page
        // bundle the tease exists to show, and turns the paper grey.
        color = BookTokens.umber(0.30f + 0.08f * t).toArgb()
        maskFilter = BlurMaskFilter(2.dp.toPx() + 7.dp.toPx() * t, BlurMaskFilter.Blur.NORMAL)
    }
    val dx = 2.dp.toPx() + 3.dp.toPx() * t
    val dy = 1.dp.toPx() + 2.dp.toPx() * t
    val s = -2.dp.toPx() * t
    val r = 5.dp.toPx()
    drawIntoCanvas {
        it.nativeCanvas.drawRoundRect(-s + dx, -s + dy, size.width + s + dx, size.height + s + dy, r, r, paint)
    }
}

/** Draws the left or right half of an open-book spread bitmap into the whole draw area. */
internal fun DrawScope.drawSpreadHalf(spread: ImageBitmap, right: Boolean) {
    val half = spread.width / 2
    drawImage(
        image = spread,
        srcOffset = IntOffset(if (right) spread.width - half else 0, 0),
        srcSize = IntSize(half, spread.height),
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
    )
}

/**
 * The spine groove — the hinge a cased-in cover swings on — plus the sheen across the board.
 * Painted over the cover art.
 *
 * Measured off the reference 3D render (book 414px wide): the groove bottoms out right at the
 * spine, the lit ridge peaks around 2.4%, and the board is flat again by 4.6%. The old ramp
 * spread the same crease over 9%, which made the board read as a soft card rather than a
 * stiff board hinged on cloth.
 */
internal fun DrawScope.drawCoverLighting(hinge: Float = 0f) {
    // [hinge] 0 shut → 1 fully ajar: the groove deepens and the ridge catches more light as the
    // board swings, which is what tells the eye it is a stiff board on a hinge, not a bent card.
    drawRect(
        Brush.horizontalGradient(
            0.000f to Color.Black.copy(alpha = 0.30f + 0.22f * hinge),
            0.016f to Color.Black.copy(alpha = 0.26f + 0.14f * hinge),
            0.024f to Color.White.copy(alpha = 0.22f + 0.14f * hinge),
            0.034f to Color.Black.copy(alpha = 0.08f),
            0.046f to Color.Transparent,
            1.000f to Color.Transparent,
        )
    )
    // 115deg sheen: light from top-left, a touch darker bottom-right
    drawRect(
        Brush.linearGradient(
            0.00f to Color.White.copy(alpha = 0.18f),
            0.45f to Color.Transparent,
            1.00f to Color.Black.copy(alpha = 0.06f),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height * 1.15f),
        )
    )
}

/**
 * The cover board's own thickness, seen edge-on through the three-quarter turn: the face of the
 * board rolls into a lit bevel, then a dark terminal edge where it turns away from the light.
 */
internal fun DrawScope.drawBoardEdge(tintLight: Color, tintDark: Color) {
    drawRect(
        Brush.horizontalGradient(
            0.00f to Color.Transparent,
            0.30f to tintLight.copy(alpha = 0.18f),
            0.70f to tintLight.copy(alpha = 0.30f),
            1.00f to tintDark.copy(alpha = 0.38f),
        )
    )
}

/**
 * The back board. Flat colour reads as a slab of card behind the cover; a board has a rolled
 * edge, so it keeps its colour across the face, loses light as it turns away at the fore-edge,
 * and sits in shade at head and tail where the cloth wraps round.
 */
internal fun DrawScope.drawBackBoard(boardColor: Color, corner: Float) {
    val r = CornerRadius(corner, corner)
    drawRoundRect(color = boardColor, cornerRadius = r)
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0.00f to Color.Transparent,
            0.82f to Color.Transparent,
            0.93f to Color.Black.copy(alpha = 0.10f),
            1.00f to Color.Black.copy(alpha = 0.30f),
        ),
        cornerRadius = r,
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            0.00f to Color.Black.copy(alpha = 0.22f),
            0.06f to Color.White.copy(alpha = 0.05f),
            0.45f to Color.Transparent,
            0.95f to Color.Black.copy(alpha = 0.08f),
            1.00f to Color.Black.copy(alpha = 0.26f),
        ),
        cornerRadius = r,
    )
}

/**
 * The text block seen on the fore-edge: a stack of ivory paper, not a striped rectangle.
 *
 * Five things do the work, and all five are needed before it reads as a physical stack:
 *  1. a **bowed** fore-edge — a stack of paper bellies out at its middle, it is never a
 *     perfectly straight rectangle;
 *  2. **ivory** across the depth, shaded where the stack turns away from the top-left light;
 *  3. leaves at **uneven pitch and brightness**, so it is paper rather than a comb;
 *  4. a visible **top plane** (near-white, caught by the light) and **bottom edge** (in shade),
 *     which is what gives the stack a measurable thickness;
 *  5. **ambient occlusion** in the two creases — where the stack meets the cover board, and
 *     where it meets the back board.
 *
 * Drawn in the same rotated plane as the cover, so it keeps the cover's perspective for free.
 */
internal fun DrawScope.drawPageBlock(tintLight: Color) {
    val w = size.width
    val h = size.height
    // The belly of the fore-edge, and the two end planes of the stack.
    val bow = w * 0.10f
    val topPlane = (h * 0.018f).coerceAtLeast(1.2f)
    val botPlane = (h * 0.016f).coerceAtLeast(1f)
    // Perspective. Measured off the render: at this turn the book's silhouette rises as it
    // recedes — over the width of the block the head lifts ~0.4% of the height and the tail
    // ~1.2%. Matching that is what stops the block reading as a rectangle pasted on the side.
    val headRise = h * 0.004f
    val tailRise = h * 0.012f
    val outer = w - bow * 0.5f

    // Silhouette: straight along the spine side, bowed along the fore-edge, and lifted at the
    // outer edge so head and tail run with the book rather than flat across it.
    val block = Path().apply {
        moveTo(0f, 0f)
        lineTo(outer, -headRise)
        quadraticBezierTo(w + bow * 0.3f, h * 0.5f - tailRise * 0.4f, outer, h - tailRise)
        lineTo(0f, h)
        close()
    }

    // Warm ivory, never white: at this size a bright edge reads as a cut-out strip rather than
    // paper. A trace of the cover tint keeps it in the same light as the book and the page.
    fun paper(c: Long, mix: Float = 0.07f) = lerp(Color(c), tintLight, mix)

    clipPath(block) {
        // Paper across the depth. Lit on the near third, rolling into warm beige as the stack
        // turns away; the first sliver is the crease hard against the cover board.
        drawRect(
            Brush.horizontalGradient(
                0.00f to paper(0xFFAE9A8E),
                0.09f to paper(0xFFD9C9BA),
                0.32f to paper(0xFFF1E4D7),
                0.56f to paper(0xFFEADCCC),
                0.82f to paper(0xFFDBC9B9),
                1.00f to paper(0xFFC2AE9E),
                startX = 0f,
                endX = w,
            )
        )

        // Leaves. Half the frequency and a third of the contrast of a printed stripe: at tile
        // size the eye only needs enough broken horizontal to read "layered", and anything
        // crisper turns the block into corduroy.
        var y = 0f
        var i = 0
        val base = 2.0.dp.toPx().coerceAtLeast(3f)
        while (y < h) {
            val hash = ((i * 1103515245 + 12345) ushr 16) and 0xFF
            val hash2 = ((i * 69069 + 5) ushr 13) and 0xFF
            val pitch = base * (0.70f + 0.60f * (hash2 / 255f))
            val crease = (pitch * 0.30f).coerceAtLeast(1f)
            // Every third leaf or so is skipped, which breaks up the rhythm far more cheaply
            // than jittering the pitch alone.
            if (hash > 70) {
                drawRect(
                    BookTokens.LeafLine.copy(alpha = 0.014f + 0.032f * (hash / 255f)),
                    Offset(0f, y),
                    Size(w, crease),
                )
                drawRect(
                    Color.White.copy(alpha = 0.015f + 0.025f * (hash2 / 255f)),
                    Offset(0f, y + crease),
                    Size(w, crease * 0.9f),
                )
            }
            y += pitch
            i++
        }

        // Roll of light across the stack, over the leaves so the whole block curves.
        drawRect(
            Brush.horizontalGradient(
                0.00f to Color.Black.copy(alpha = 0.10f),
                0.30f to Color.White.copy(alpha = 0.10f),
                0.64f to Color.Transparent,
                1.00f to Color.Black.copy(alpha = 0.10f),
                startX = 0f,
                endX = w,
            )
        )

        // Ambient occlusion in the crease where the cover board closes on the stack. This is
        // the contact that sells the depth: without it cover and paper look like two decals.
        drawRect(
            Brush.horizontalGradient(
                0.00f to BookTokens.umber(0.42f),
                0.35f to BookTokens.umber(0.14f),
                1.00f to Color.Transparent,
                startX = 0f,
                endX = w * 0.30f,
            ),
            Offset.Zero,
            Size(w * 0.30f, h),
        )

        // Ambient occlusion where the boards close on the stack at head and tail, kept inside
        // the plane depths below so it shades the stack without swallowing its end faces.
        drawRect(
            Brush.verticalGradient(
                0.00f to BookTokens.umber(0.20f),
                0.10f to Color.Transparent,
                0.90f to Color.Transparent,
                1.00f to BookTokens.umber(0.20f),
                startY = topPlane,
                endY = h - botPlane,
            ),
            Offset(0f, topPlane),
            Size(w, h - topPlane - botPlane),
        )
    }

    // The two end planes, drawn as their own slanted shapes so they sit on the book's
    // perspective instead of running flat across it. Top is the face you look down on; the
    // bottom is the same plane seen from under, so it stays in shade.
    val top = Path().apply {
        moveTo(0f, 0f)
        lineTo(outer, -headRise)
        lineTo(outer, -headRise + topPlane * 1.8f)
        lineTo(0f, topPlane * 1.8f)
        close()
    }
    clipPath(top) {
        drawRect(
            Brush.verticalGradient(
                0.00f to BookTokens.umber(0.30f),
                0.28f to paper(0xFFF6EBDE, 0.05f),
                1.00f to Color(0x00F6EBDE),
                startY = -headRise,
                endY = -headRise + topPlane * 1.8f,
            )
        )
    }
    val bottom = Path().apply {
        moveTo(0f, h - botPlane * 2.0f)
        lineTo(outer, h - tailRise - botPlane * 2.0f)
        lineTo(outer, h - tailRise)
        lineTo(0f, h)
        close()
    }
    clipPath(bottom) {
        drawRect(
            Brush.verticalGradient(
                0.00f to Color(0x00D9CBB8),
                0.45f to paper(0xFFD9CBB8, 0.05f),
                1.00f to paper(0xFF93826F, 0.05f),
                startY = h - botPlane * 2.0f,
                endY = h,
            )
        )
    }
}

/**
 * Horizontal leaf creases over whatever is already painted: a dark crease, then the lit face of
 * the next sheet, with a wandering pitch so the eye reads paper rather than a printed stripe.
 */
private fun DrawScope.drawLeafCreases(w: Float, h: Float) {
    var y = 0f
    var i = 0
    val base = 0.95.dp.toPx().coerceAtLeast(1.6f)
    while (y < h) {
        val hash = ((i * 1103515245 + 12345) ushr 16) and 0xFF
        val hash2 = ((i * 69069 + 5) ushr 13) and 0xFF
        val pitch = base * (0.80f + 0.45f * (hash2 / 255f))
        val crease = (pitch * 0.32f).coerceAtLeast(0.7f)
        drawRect(
            BookTokens.LeafLine.copy(alpha = 0.030f + 0.075f * (hash / 255f)),
            Offset(0f, y),
            Size(w, crease),
        )
        drawRect(
            Color.White.copy(alpha = 0.04f + 0.07f * (hash2 / 255f)),
            Offset(0f, y + crease),
            Size(w, crease * 0.9f),
        )
        y += pitch
        i++
    }
}

/**
 * What the tease reveals as the cover lifts: more of the **same text block** that shows on the
 * fore-edge, not a second material.
 *
 * Two rules here, both learned the hard way:
 *  - whatever is *not* paper is the dark inside of the board, so nothing can read as a stray
 *    pale sheet poking out past the book
 *  - the paper carries the same ivory and the same horizontal leaf creases as [drawPageBlock]
 *    right beside it, so the reveal and the fore-edge join into one thick stack. Stepped sheet
 *    outlines here just read as corrugation at this size.
 */
internal fun DrawScope.drawInnerLeaf(tintDark: Color, boardColor: Color) {
    // The board behind the paper, in its own shade.
    drawRect(lerp(boardColor, Color.Black, 0.28f))

    // Squares: the boards stand proud of the text block at head and tail. Nothing is taken off
    // the fore-edge, so the paper runs straight under [drawPageBlock] — a gap there puts a dark
    // line between the two and the stack breaks back into two separate objects.
    val sq = (size.height * 0.013f).coerceAtLeast(1f)
    inset(left = 0f, top = sq, right = 0f, bottom = sq) {
        val w = size.width
        val h = size.height
        drawRect(
            Brush.horizontalGradient(
                0.00f to Color(0xFFBCA794),
                0.18f to Color(0xFFE6DCCB),
                0.46f to Color(0xFFFBF5EA),
                0.80f to Color(0xFFFFFDF8),
                1.00f to Color(0xFFF4ECDD),
                startX = 0f,
                endX = w,
            )
        )
        drawLeafCreases(w, h)
        // Head and tail fall into shade where the boards close on the stack.
        drawRect(
            Brush.verticalGradient(
                0.00f to BookTokens.umber(0.20f),
                0.10f to Color.Transparent,
                0.90f to Color.Transparent,
                1.00f to BookTokens.umber(0.20f),
                startY = 0f,
                endY = h,
            )
        )
        // The gutter: the shadow the cover's board throws into the book along the spine. Tinted
        // with the cover so the two read as the same object.
        drawRect(
            Brush.horizontalGradient(
                0.00f to lerp(tintDark, Color.Black, 0.35f).copy(alpha = 0.78f),
                0.55f to tintDark.copy(alpha = 0.20f),
                1.00f to Color.Transparent,
                startX = 0f,
                endX = w * 0.26f,
            ),
            Offset.Zero,
            Size(w * 0.26f, h),
        )
    }
}

/** Soft elliptical shadow (radial gradient), used for the cast shadow and the open book's contact shadow. */
internal fun DrawScope.drawEllipseShadow(color: Color) {
    // A circular radial gradient squashed vertically, so it fades to nothing on every side.
    val r = size.width / 2f
    scale(scaleX = 1f, scaleY = size.height / size.width, pivot = center) {
        drawCircle(
            Brush.radialGradient(
                0.00f to color,
                0.72f to color.copy(alpha = 0f),
                1.00f to Color.Transparent,
                center = center,
                radius = r,
            ),
            radius = r,
            center = center,
        )
    }
}

/** True when the user has turned animations off (Developer options / Accessibility → Remove animations). */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
