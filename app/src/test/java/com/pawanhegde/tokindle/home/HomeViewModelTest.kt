package com.pawanhegde.tokindle.home

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

internal class HomeViewModelTest {
    @ParameterizedTest
    @CsvSource(
        "0,a few moments ago",
        "1,a few moments ago",
        "60000,about a minute ago",
        "119000,about a minute ago",
        "120000,about 2 minutes ago",
        "360000,about 6 minutes ago",
        "3600000,about an hour ago",
        "7200000,about 2 hours ago",
        "86400000,about a day ago",
        "172800000,about 2 days ago"
    )
    fun testToRelativeDuration(durationInMilis: Long, relativeDuration: String) {
        assertThat(HomeViewModel.toRelativeDuration(durationInMilis)).isEqualTo(relativeDuration)
    }

    @OptIn(ExperimentalTime::class)
    @ParameterizedTest
    @CsvSource(
        "1,about a day ago",
        "2,about 2 days ago",
        "60,about 60 days ago",
        "400,quite some time ago"
    )
    fun testToRelativeDuration_LongDurations(durationInDays: Int, relativeDuration: String) {
        assertThat(
            HomeViewModel.toRelativeDuration(
                durationInDays.toDuration(DurationUnit.DAYS).toLong(DurationUnit.MILLISECONDS)
            )
        ).isEqualTo(relativeDuration)
    }
}