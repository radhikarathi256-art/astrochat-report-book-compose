package com.astrochat.reports.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** All four books on one row, fluttering one at a time (Today "Personalised Guidance" / shelf). */
@Preview(name = "Closed books · flutter one at a time", widthDp = 360, heightDp = 160, showBackground = true)
@Composable
private fun PreviewFlutterShelf() {
    BookFlutterGroup(bookCount = ReportBook.entries.size) {
        Row(
            Modifier.background(Color.White).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            ReportBook.entries.forEachIndexed { i, book ->
                ReportBookCover(book = book, size = BookSize.Upsell, flutterSlot = i)
            }
        }
    }
}

/** Every size the closed book is used at. */
@Preview(name = "Closed book · sizes", widthDp = 360, heightDp = 180, showBackground = true)
@Composable
private fun PreviewSizes() {
    Row(
        Modifier.background(Color.White).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BookSize.entries.forEach { ReportBookCover(book = ReportBook.KundliMilan, size = it, flutter = false) }
    }
}

/** Detail page hero, open end state, one per report (run in Interactive mode to see the opening). */
@Preview(name = "Detail hero · open book · all reports", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun PreviewHeroes() {
    Column(
        Modifier.background(Color(0xFFFAF6EF)).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ReportBook.entries.forEach { book ->
            ReportBookOpenHero(book = book, onSampleReportClick = {}, animate = false)
        }
    }
}

@Preview(name = "Detail hero · opening (interactive)", widthDp = 360, heightDp = 220, showBackground = true)
@Composable
private fun PreviewHeroOpening() {
    Column(Modifier.background(Color(0xFFFAF6EF)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ReportBookOpenHero(book = ReportBook.Relationships, onSampleReportClick = {})
    }
}
