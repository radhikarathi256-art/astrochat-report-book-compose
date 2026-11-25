# AstroChat · Report book (Jetpack Compose)

Ready-to-use components for the paid-report book, matching the report prototype
(`astrochat-report-demo`) and Figma Hackfest 2026:

| Component | Where | What it does |
|---|---|---|
| `ReportBookCover(book, size, flutter, flutterSlot)` | Today tile, "You may also like", My Reports, chat/History widget, pay sheet | Closed **hardcover** standing at a ¾ turn (board, squares, page block, board edge, spine groove, cast shadow) with the **page flutter** tease |
| `BookFlutterGroup(bookCount) { … }` | Any screen listing several books | One clock so books flutter **one at a time** (slot 0, then 1, …) |
| `ReportBookOpenHero(book, onSampleReportClick)` | Detail page hero | Book **opens once** when the page opens, then rests open with the **See Sample report** pill, contact shadow and gutter |
| `SampleReportPill(onClick)` | Detail hero (also usable alone) | White pill, eye icon, umber shadow |

`ReportBook` = `MarriagePrediction`, `KundliMilan`, `CareerMoney`, `Relationships` — each carries
its cover, open-book spread and cover tints (board + shadow colours are mixed from them).

## Setup

1. Copy `src/com/astrochat/reports/book/*.kt` into the app module (change the package and the
   `com.astrochat.R` import if needed).
2. Copy `res/drawable-nodpi/*.webp` into `app/src/main/res/drawable-nodpi/`.
3. Needs Compose UI + Foundation + Material3 (only `Text`). No other libraries.

```kotlin
// Today → Personalised Guidance / shelf of books
BookFlutterGroup(bookCount = reports.size) {
    reports.forEachIndexed { i, r -> ReportBookCover(book = r.book, size = BookSize.Tile, flutterSlot = i) }
}

// Detail page hero
ReportBookOpenHero(book = ReportBook.KundliMilan, onSampleReportClick = { openSample() })
```

## Motion (1:1 from the prototype CSS)

**Flutter — `@keyframes bookAjar`, 3.6s loop, cubic-bezier(.33,1,.68,1) per segment**

| Time | Cover angle |
|---|---|
| 0 – 0.94s (0–26%) | 0° (shut) |
| 1.44s (40%) | −36° |
| 1.73s → 2.95s (48–82%) | −30° (hold) |
| 3.46s → 3.6s (96–100%) | 0° |

First delay 0.7s (rows) / 1.1s (tile). In a group each book uses the 2.52s moving window in turn.

**Opening on the detail page (once)**

| Time | Beat |
|---|---|
| 0.12 → 0.62s | Rise: fade in, 14dp up, scale .97 → 1 (cubic-bezier .22,.9,.3,1) |
| 0.90 → 2.80s | Cover 0° → −184° (78%) → −180°; book slides right half a page so the spread centres |
| 2.50 → 3.00s | Contact shadow + gutter fade in |
| 2.60 → 2.95s | "See Sample report" pill fades in, straddling the foot of the book (4dp below) |

Animations off in system settings → static (closed books shut, hero shown open).
Pass `animate = false` to `ReportBookOpenHero` to skip the opening (e.g. returning to the page in the same session).

## Sizes

| `BookSize` | Size | Block depth |
|---|---|---|
| Tile | 42 × 63 | 9 |
| Upsell | 58 × 87 | 12 |
| ChatWidget | 61 × 91 | 13 |
| Standing | 86 × 129 | 18 |

Every face is **2:3**, the aspect the cover art is drawn at in Figma. Anything else and
`ContentScale.Crop` eats the title block off the top and bottom.

`depth` ≈ width × 0.21 at every size, so after the 26° turn the ivory fore-edge lands at
**~10% of the face width**.

Hero spread: 183 × 122 (page 91.5 × 122) — override with `pageWidth` / `pageHeight`.

## How the hardcover is built

Measured off the reference 3D book render (book 414px wide):

| Part | Value | Why it reads as a hardcover |
|---|---|---|
| Page block (fore-edge) | ~10% of width, warm ivory, bowed, with a lit top plane and a shaded bottom edge | A stack of paper with volume, not a white strip |
| Board rim past the pages | 2.0% of width, back-board colour | Boards are bigger than the text block — a case, not a card |
| Squares (head & tail) | 1.6% of height each | Same, top and bottom |
| Cover board thickness | 2.2% of width, lit bevel → dark terminal edge | The board has a side |
| Spine groove | inside the first 4.6%: dark 30% → white ridge 22% → flat | Stiff board hinged on cloth. Deepens to 52% as the cover lifts |

## Shadows & colours

- Cast shadow under a standing book: soft ellipse (102% of width × 13dp, cover tint 42%) offset
  **right** of centre, plus a tight `rgba(74,24,8,.34)` smear under the foot. Light is top-left.
- Cover shadow onto the page (`coverLiftShadow`): tracks the flutter in the draw phase —
  shut `2/1` offset, 2 blur, `.34`; fully ajar `10/4`, 13 blur, −2 spread, `.56`.
- Back board: cover tint 38% over `#2A1016`. Page block: cream with fine leaf lines.
- Open book (Figma spec): contact shadow `rgba(74,24,8,.34)` ellipse **168 × 18 at (16.5, 122)**,
  i.e. below the foot and 9 right of centre; gutter **27 × 122 at x = 78**, full page height,
  `rgba(142,97,72, 0 / .30@44% / .36@50% / .30@56% / 0)` multiply.
- Pill: white, radius 15, `0 4 12 rgba(74,24,8,.22)` + `0 1 2 rgba(74,24,8,.14)`, text 12 SemiBold `#101828`.

| Report | Tint light | Tint dark |
|---|---|---|
| Marriage Prediction | #F0668B | #8B2F55 |
| Kundli Milan | #E8973C | #8A4A12 |
| Career & Money | #3E8FD6 | #1B4A78 |
| Relationships | #9B6CD1 | #4A2A7A |

## Assets (`res/drawable-nodpi`)

Re-exported 25 Nov 2026 from Figma "Hackfest - 2026" (file `XxK1Uuqr7WYELH1Ba0WZKm`), replacing
the 19 Nov set.

| File | Figma node | Size |
|---|---|---|
| report_cover_marriage / kundli / career / relationships | 4723:97414 · 4723:100783 · 4723:102303 · 4723:104145 | 400 × 600 |
| report_spread_marriage / kundli / career / relationships | 4723:97411 · 4723:100758 · 4723:104123 · 4723:104151 | 1000 × 632 |

Spreads are cropped to the book's own outer edges (the Figma frames carry a transparent margin),
so `drawSpreadHalf` can split them down the gutter and stretch each half onto a page.

Built and checked on a device (compileSdk 35, Compose BOM 2024.10.01). Open `ReportBookPreviews.kt`
in Android Studio to see every book and size.
