package kattcrazy.calendartoalarm

sealed interface SyncResult {
    data class Success(val scheduledCount: Int) : SyncResult
    data object NotReady : SyncResult
    data object NotEnabled : SyncResult
    data class Error(val message: String) : SyncResult
}
