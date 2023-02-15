package com.example.evrecharge

import android.Manifest.permission.CALL_PHONE
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.evrecharge.MapsActivityTest.Companion.context

var locList = ArrayList<StationDataModel>()
var hasImage:Boolean = true
var myActivity: AppCompatActivity? = null

class CustomAdapter(private var stationList: ArrayList<StationDataModel>, private var act:AppCompatActivity, private val optVal: Boolean = true) :
    RecyclerView.Adapter<CustomAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.station_info, parent, false)
        hasImage = optVal
        myActivity = act
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(stationList[position])
        locList = stationList
    }

    override fun getItemCount(): Int {
        return stationList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        fun bindItems(station: StationDataModel) {
            val stationName = itemView.findViewById(R.id.station_name) as TextView
            val stationAddress = itemView.findViewById(R.id.station_address) as TextView
            val stationImage = itemView.findViewById(R.id.station_image) as ImageView
            val callAtStation = itemView.findViewById(R.id.call) as Button
            val payStation = itemView.findViewById(R.id.pay) as Button

            stationName.text = station.name
            stationAddress.text = station.address
            if(hasImage) {
                if (station.imagePath.isNotEmpty())
                    Glide.with(context).load(station.imagePath).into(stationImage) else
                    Glide.with(context).load(R.drawable.default_moto_station).into(stationImage)
            }
            else {
                stationImage.visibility = View.GONE
            }
            callAtStation.id = station.stationId
            payStation.id = station.stationId

            payStation.setOnClickListener { view ->
                val intent = Intent(context, PaymentActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra("stationId", view.id)
                context.startActivity(intent)
            }

            callAtStation.setOnClickListener { v ->
//                redirect station mobile no to call
                val callIntent = Intent(Intent.ACTION_DIAL)
                callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                callIntent.data = Uri.parse("tel:${locList[v.id].mobileNo}")

//                request phone call permission in adapter
                if (ActivityCompat.checkSelfPermission(context, CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(myActivity!!, arrayOf(CALL_PHONE), 1)
                    return@setOnClickListener
                }
                startActivity(context, callIntent, null)
            }
        }

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(loc: View?) {
            locList[position].let {
                MapsActivityTest().getPosition(it, context)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filterList(filteredNames: ArrayList < StationDataModel > ) {
        this.stationList = filteredNames
        notifyDataSetChanged()
    }
}