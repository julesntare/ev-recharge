package com.example.evrecharge

data class VisitedStationsDataModel(
    val station_name: String,
    val user_id: String,
    val imagePath: String,
    val total_paid: Int,
    val payment_method: String,
    val visited_date: String,
)
