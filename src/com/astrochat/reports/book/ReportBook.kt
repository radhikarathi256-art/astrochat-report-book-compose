package com.astrochat.reports.book

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.astrochat.R

/**
 * The four paid reports, each with its own book: painted cover, open-book spread and
 * the two cover tints the 3D book mixes its back board and cast shadow from.
 *
 * Source: AstroChat report prototype (astrochat-report-demo, `REPORTS[].cover` + `art`)
 * and Figma Hackfest 2026 → "Open book · Detail hero" (3785:11536) /
 * "Sell · Closed book · proto art" (3689:16800).
 */
enum class ReportBook(
    @DrawableRes val coverRes: Int,
    @DrawableRes val spreadRes: Int,
    /** Lighter cover tint (`cover.a` in the prototype). */
    val tintLight: Color,
    /** Darker cover tint (`cover.b` in the prototype). Spine colour during the turn. */
    val tintDark: Color,
    val contentDescription: String,
) {
    MarriagePrediction(
        coverRes = R.drawable.report_cover_marriage,
        spreadRes = R.drawable.report_spread_marriage,
        tintLight = Color(0xFFF0668B),
        tintDark = Color(0xFF8B2F55),
        contentDescription = "Marriage Prediction Report",
    ),
    KundliMilan(
        coverRes = R.drawable.report_cover_kundli,
        spreadRes = R.drawable.report_spread_kundli,
        tintLight = Color(0xFFE8973C),
        tintDark = Color(0xFF8A4A12),
        contentDescription = "Kundli Milan Report",
    ),
    CareerMoney(
        coverRes = R.drawable.report_cover_career,
        spreadRes = R.drawable.report_spread_career,
        tintLight = Color(0xFF3E8FD6),
        tintDark = Color(0xFF1B4A78),
        contentDescription = "Career & Money Report",
    ),
    Relationships(
        coverRes = R.drawable.report_cover_relationships,
        spreadRes = R.drawable.report_spread_relationships,
        tintLight = Color(0xFF9B6CD1),
        tintDark = Color(0xFF4A2A7A),
        contentDescription = "Relationship Report",
    );

    /** Back board: cover tint at 38% over a deep brown, so a thin dark rim shows past the pages. */
    val boardColor: Color get() = lerp(Color(0xFF2A1016), tintLight, 0.38f)

    /** Cast shadow under a standing book: the cover tint at 42%. */
    val castShadowColor: Color get() = tintLight.copy(alpha = 0.42f)
}

/** Shared colours and motion constants, taken 1:1 from the prototype CSS. */
internal object BookTokens {
    /** cubic-bezier(.33,1,.68,1): every turn of the cover. */
    val TurnEasing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

    /** cubic-bezier(.22,.9,.3,1): the hero book rising into place. */
    val RiseEasing = CubicBezierEasing(0.22f, 0.9f, 0.3f, 1f)

    // Paper
    val Paper = Color(0xFFFFFDF8)
    val PaperWarm = Color(0xFFF0E2D2)
    val PaperEdge = Color(0xFFE3D7C0)
    val LeafLine = Color(0x4D78604A)          // rgba(120,96,70,.30)
    // Fore-edge cream, measured off the reference 3D render: (239,234,223) lit, (222,215,202) shaded.
    val PageBlockLight = Color(0xFFFFFCF5)
    val PageBlockDark = Color(0xFFDED7CA)

    // Shadows, all from rgba(74,24,8,a)
    val Umber = Color(0xFF4A1808)
    fun umber(alpha: Float) = Umber.copy(alpha = alpha)

    // Gutter of the open spread: rgba(142,97,72,a), multiplied
    val Gutter = Color(0xFF8E6148)

    // "See Sample report" pill
    val PillText = Color(0xFF101828)
    val PillPressed = Color(0xFFF2F4F7)

    // ---- Flutter (closed book tease) — @keyframes bookAjar, 3.6s loop ----
    const val FLUTTER_CYCLE_MS = 3600

    /** Part of the cycle in which the cover actually moves (26% → 96%). */
    const val FLUTTER_MOTION_MS = 2520
    const val FLUTTER_OPEN_DEG = -44f
    const val FLUTTER_HOLD_DEG = -37f

    /** Resting tilt of a standing book. Turned far enough that the thickness reads at tile size. */
    const val STAND_TILT_Y = -26f
    const val STAND_TILT_X = 2f

    // ---- Hero opening on the detail page (R.6) ----
    const val RISE_DELAY_MS = 120
    const val RISE_MS = 500
    const val OPEN_DELAY_MS = 800
    const val OPEN_MS = 1500
    // Gutter + contact shadow come in *while* the cover is still coming down, so the spine
    // forms as the book opens instead of appearing after it has already settled.
    const val SEAT_DELAY_MS = 1450
    const val SEAT_MS = 650
    const val PILL_DELAY_MS = 2150            // "See Sample report" fades in
    const val PILL_MS = 350
}
