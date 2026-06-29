package com.bipolarmood

import com.bipolarmood.data.DiaryEntryEntity
import com.bipolarmood.data.MoodEntryEntity

sealed class FeedItem {
    abstract val timestamp: Long
    abstract val sortKey: String

    data class Note(val entry: DiaryEntryEntity) : FeedItem() {
        override val timestamp: Long = entry.timestamp
        override val sortKey: String = "note-${entry.id}"
    }

    data class Mood(val entry: MoodEntryEntity) : FeedItem() {
        override val timestamp: Long = entry.timestamp
        override val sortKey: String = "mood-${entry.id}"
    }
}

enum class FeedFilter(val label: String) {
    All("Всё"),
    Notes("Записи"),
    Moods("Состояния")
}

fun buildFeedItems(
    diaryEntries: List<DiaryEntryEntity>,
    moodEntries: List<MoodEntryEntity>
): List<FeedItem> {
    val notes = diaryEntries.map { FeedItem.Note(it) }
    val moods = moodEntries.map { FeedItem.Mood(it) }
    return (notes + moods).sortedByDescending { it.timestamp }
}

fun List<FeedItem>.applyFilter(filter: FeedFilter): List<FeedItem> = when (filter) {
    FeedFilter.All -> this
    FeedFilter.Notes -> filterIsInstance<FeedItem.Note>()
    FeedFilter.Moods -> filterIsInstance<FeedItem.Mood>()
}
