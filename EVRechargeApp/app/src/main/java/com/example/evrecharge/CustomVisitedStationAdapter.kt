package com.example.evrecharge

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.evrecharge.MapsActivityTest.Companion.context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

var visitedLocList = ArrayList<VisitedStationsDataModel>()
var hasVisitedImage:Boolean = true

class CustomVisitedStationAdapter(private var visitedStationList: ArrayList<VisitedStationsDataModel>, private val optVal: Boolean = true) :
    RecyclerView.Adapter<CustomVisitedStationAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.visited_station_info, parent, false)
        hasVisitedImage = optVal
        return ViewHolder(v)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(visitedStationList[position])
        visitedLocList = visitedStationList
    }

    override fun getItemCount(): Int {
        return visitedStationList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @RequiresApi(Build.VERSION_CODES.O)
        fun bindItems(station: VisitedStationsDataModel) {
            val visitedStationImage = itemView.findViewById(R.id.visited_station_image) as ImageView
            val visitedStationName = itemView.findViewById(R.id.visited_station_name) as TextView
            val visitedStationDate = itemView.findViewById(R.id.visited_station_date) as TextView
            val visitedStationAmountPaid = itemView.findViewById(R.id.visited_station_amount_paid) as TextView
            val visitedStationPaymentMethod = itemView.findViewById(R.id.visited_station_payment_method) as TextView
            visitedStationName.text = station.station_name
            visitedStationDate.text = station.visited_date
            visitedStationAmountPaid.text = "${station.total_paid} Rwf"
            visitedStationPaymentMethod.text = station.payment_method
            if(hasVisitedImage) {
                if (station.imagePath.isNotEmpty())
                    Glide.with(context).load(station.imagePath).into(visitedStationImage) else
                    Glide.with(context).load(R.drawable.default_moto_station).into(visitedStationImage)
            }
            else {
                visitedStationImage.visibility = View.GONE
            }
        }
    }
}