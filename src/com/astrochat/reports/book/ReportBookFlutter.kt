package com.astrochat.reports.book

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sizes the closed book appears at across the app (prototype CSS / Figma).
 *
 * Every face is **2:3**, the aspect the cover art is drawn at in Figma (720 × 1078). Anything
 * else and `ContentScale.Crop` eats the title block off the top and bottom of the art.
 *
 * `depth` is the thickness of the cased-in text block. Through the turn it foreshortens to
 * `depth × sin 26°`, putting the cream fore-edge at ~9.5% of the face width.
 */
enum class BookSize(val width: Dp, val height: Dp, val depth: Dp) {
    /** Today → Personalised Guidance tile. */
    Tile(42.dp, 63.dp, 9.dp),

    /** "You may also like" / My Reports rows. */
    Upsell(58.dp, 87.dp, 12.dp),

    /** Chat / History report widget, book leaning out of the bubble. */
    ChatWidget(61.dp, 91.dp, 13.dp),

    /** Large standing book (pay sheet, picker). */
    Standing(86.dp, 129.dp, 18.dp),
}

/**
 * A closed report book, standing at a three-quarter turn so its page block shows, with the
 * page-flutter tease: the cover lifts to -36°, settles to -30°, holds the glance, then shuts.
 *
 * Several books on one screen must flutter **one at a time** — wrap them in [BookFlutterGroup]
 * and give each a different [flutterSlot]. Outside a group the book runs its own 3.6s loop.
 *
 * Covers are static (no flutter) when [flutter] is false or the system has animations off.
 */
@Composable
fun ReportBookCover(
    book: ReportBook,
    modifier: Modifier = Modifier,
    size: BookSize = BookSize.Upsell,
    flutter: Boolean = true,
    flutterSlot: Int = 0,
    /** Only used outside a [BookFlutterGroup]: delay before the first flutter (tile 1100, rows 700). */
    flutterDelayMillis: Int = 700,
) {
    val reduceMotion = rememberReduceMotion()
    val animate = flutter && !reduceMotion
    val group = LocalBookFlutterClock.current

    // Angle of the cover, in degrees (0 = shut, negative = opening towards the viewer's left).
    val angle: State<Float> = when {
        !animate -> remember { mutableFloatStateOf(0f) }
        group != null -> group.angleFor(flutterSlot)
        else -> {
            val t = rememberInfiniteTransition(label = "bookFlutter")
            val ms = t.animateFloat(
                initialValue = 0f,
                targetValue = BookTokens.FLUTTER_CYCLE_MS.toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(BookTokens.FLUTTER_CYCLE_MS, easing = LinearEasing),
                    initialStartOffset = StartOffset(flutterDelayMillis),
                ),
                label = "flutterClock",
            )
            remember(ms) { derived { flutterAngle(ms.value) } }
        }
    }

    ClosedBook(
        book = book,
        size = size,
        coverAngle = { angle.value },
        modifier = modifier.semantics { contentDescription = book.contentDescription },
    )
}

/**
 * Runs one clock for a set of books so only one flutters at a time (e.g. the 4 reports
 * listed on Today or on a detail page's "You may also like"). Slot i flutters, then i+1.
 */
@Composable
fun BookFlutterGroup(
    bookCount: Int,
    startDelayMillis: Int = 700,
    content: @Composable () -> Unit,
) {
    val count = bookCount.coerceAtLeast(1)
    // Each book gets the moving part of the 3.6s tease (2.52s); with one book, keep the full rest.
    val period = maxOf(BookTokens.FLUTTER_CYCLE_MS, count * BookTokens.FLUTTER_MOTION_MS)
    val t = rememberInfiniteTransition(label = "bookFlutterGroup")
    val ms = t.animateFloat(
        initialValue = 0f,
        targetValue = period.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(period, easing = LinearEasing),
            initialStartOffset = StartOffset(startDelayMillis),
        ),
        label = "flutterGroupClock",
    )
    val clock = remember(ms, count, period) { BookFlutterClock(ms, count, period) }
    CompositionLocalProvider(LocalBookFlutterClock provides clock, content = content)
}

internal class BookFlutterClock(private val ms: State<Float>, private val count: Int, private val period: Int) {
    private val cache = HashMap<Int, State<Float>>()
    fun angleFor(slot: Int): State<Float> = cache.getOrPut(slot) {
        derived {
            val local = (ms.value - (slot % count) * BookTokens.FLUTTER_MOTION_MS).mod(period.toFloat())
            // The motion window of bookAjar runs 936ms → 3456ms of its own cycle.
            if (local < BookTokens.FLUTTER_MOTION_MS) flutterAngle(936f + local) else 0f
        }
    }
}

internal val LocalBookFlutterClock = compositionLocalOf<BookFlutterClock?> { null }

private fun <T> derived(calc: () -> T): State<T> = androidx.compose.runtime.derivedStateOf(calc)

/**
 * @keyframes bookAjar (3.6s), with cubic-bezier(.33,1,.68,1) on every segment:
 *   0–26% shut · 40% -36° · 48–82% -30° · 96–100% shut.
 */
internal fun flutterAngle(ms: Float): Float {
    fun seg(t0: Float, t1: Float, from: Float, to: Float): Float {
        val p = ((ms - t0) / (t1 - t0)).coerceIn(0f, 1f)
        return from + (to - from) * BookTokens.TurnEasing.transform(p)
    }
    return when {
        ms < 936f -> 0f
        ms < 1440f -> seg(936f, 1440f, 0f, BookTokens.FLUTTER_OPEN_DEG)
        ms < 1728f -> seg(1440f, 1728f, BookTokens.FLUTTER_OPEN_DEG, BookTokens.FLUTTER_HOLD_DEG)
        ms < 2952f -> BookTokens.FLUTTER_HOLD_DEG
        ms < 3456f -> seg(2952f, 3456f, BookTokens.FLUTTER_HOLD_DEG, 0f)
        else -> 0f
    }
}

/**
 * The standing book, built like a real cased-in hardcover rather than a flat card:
 *
 *  - two **squares**: the boards stand proud of the text block at head, tail and fore-edge,
 *    so a thin rim of board shows past the cream page block on all three free sides
 *  - a **page block** on the fore-edge at 7.25% of the face width (reference 3D render)
 *  - the cover's own **board thickness** caught by the light along its fore-edge
 *  - a **spine groove** (the hinge the cover swings on) inside the first 4.6% of the face
 *  - a shadow that falls right and down, heaviest where the foot touches
 *
 * The whole case is turned -22° so all of that reads as depth.
 */
@Composable
internal fun ClosedBook(
    book: ReportBook,
    size: BookSize,
    coverAngle: () -> Float,
    modifier: Modifier = Modifier,
) {
    val coverShape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 5.dp, bottomEnd = 5.dp)
    // How far the case shows past the cover through the turn (sin 26° ≈ .44).
    val edge = size.depth * 0.52f
    // Squares: board overhang at head/tail, and past the fore-edge (reference: 0.9% of width).
    val square = (size.height * 0.016f).coerceAtLeast(1.dp)
    val rim = (size.width * 0.020f).coerceAtLeast(1.25.dp)
    // The cover board's own thickness, seen edge-on through the turn.
    val boardEdge = (size.width * 0.022f).coerceAtLeast(1.dp)

    Box(modifier.size(size.width + edge + rim, size.height + 14.dp)) {
        // 1. Cast shadow. Light is top-left (see drawCoverLighting), so it falls to the right,
        //    and it is darkest in the narrow smear where the foot of the book meets the surface.
        Canvas(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(x = edge * 0.6f, y = (-3).dp)
                .requiredSize(size.width * 1.02f, 13.dp)
        ) { drawEllipseShadow(book.castShadowColor) }
        Canvas(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(x = edge * 0.3f, y = (-9).dp)
                .requiredSize(size.width * 0.72f, 6.dp)
        ) { drawEllipseShadow(BookTokens.umber(0.34f)) }

        // 2. The case, turned three-quarters.
        Box(
            Modifier
                .requiredSize(size.width, size.height)
                .align(Alignment.TopStart)
                .graphicsLayer {
                    rotationY = BookTokens.STAND_TILT_Y
                    rotationX = BookTokens.STAND_TILT_X
                    cameraDistance = 10f * density
                }
        ) {
            // Back board. One block-depth *plus a square* behind the cover, so a dark rim of
            // board shows past the cream fore-edge — the giveaway of a cased-in hardcover.
            Canvas(
                Modifier
                    .offset(x = edge + rim)
                    .fillMaxSize()
            ) { drawBackBoard(book.boardColor, 4.dp.toPx()) }
            // Text block on the fore-edge, inset from head and tail by the squares. It is drawn
            // wider than the foreshortened depth: a real stack of paper is the thickest thing
            // on this edge, and at tile size a hairline strip just reads as a seam.
            Canvas(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = edge)
                    .requiredSize(edge * 1.15f, size.height - square * 1.4f)
            ) { drawPageBlock(book.tintLight) }
            // The endpaper the cover lifts off (only seen during the flutter), inset the same way.
            Canvas(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 1.dp, bottomStart = 1.dp, topEnd = 3.dp, bottomEnd = 3.dp))
            ) { drawInnerLeaf(book.tintDark, book.boardColor) }
            // Cover, hinged on the spine. Its shadow falls onto the page as it lifts.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationY = coverAngle()
                        cameraDistance = 10f * density
                    }
                    .coverLiftShadow(coverAngle)
                    .clip(coverShape)
            ) {
                Image(
                    painter = painterResource(book.coverRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Canvas(Modifier.fillMaxSize()) {
                    drawCoverLighting((coverAngle() / BookTokens.FLUTTER_OPEN_DEG).coerceIn(0f, 1f))
                }
                // The board's own thickness along the fore-edge: lit bevel, dark terminal edge.
                Canvas(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .requiredWidth(boardEdge)
                ) { drawBoardEdge(book.tintLight, book.tintDark) }
            }
        }
    }
}
