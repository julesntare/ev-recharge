package com.example.evrecharge

import com.google.android.gms.maps.model.LatLng

data class StationDataModel(
    val stationId: Int,
    val stationRefId: String,
    val name: String,
    val latLng: LatLng,
    val address: String,
    val imagePath: String,
    val mobileNo: String
)
