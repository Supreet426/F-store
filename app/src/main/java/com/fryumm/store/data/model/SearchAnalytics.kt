package com.fryumm.store.data.model

data class SearchEvent(
    val id: String = "",
    val query: String = "",
    val userId: String = "user_anon",
    val area: String = "Main Zone",
    val timestamp: Long = System.currentTimeMillis(),
    val resultCount: Int = 0,
    val sessionDurationSeconds: Int = 0
)

data class PopularSearch(
    val query: String = "",
    val count: Int = 0,
    val uniqueUsers: Int = 0
)

data class AreaSearchDemand(
    val area: String = "",
    val topQueries: List<String> = emptyList(),
    val totalSearches: Int = 0
)

data class UserSearchProfile(
    val userId: String = "",
    val lastSearches: List<String> = emptyList(),
    val frequentQueries: List<String> = emptyList()
)

data class UserUsageProfile(
    val userId: String = "",
    val totalTimeSeconds: Int = 0,
    val sessionCount: Int = 0
)

data class SubscriptionStats(
    val subscribedOrderCount: Int = 0,
    val subscribedRevenue: Double = 0.0,
    val nonSubscribedOrderCount: Int = 0,
    val nonSubscribedRevenue: Double = 0.0
)
