package com.example.evrecharge

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

open class CurrentLocationAccess(private val mContext: AppCompatActivity) : Service(), LocationListener {
    private var longitude: Double = 0.0
    private var latitude: Double = 0.0
    private var locationManager: LocationManager? = null
    private var checkGPS = false
    private var checkNetwork = false
    private var canGetLocation = false
    private var loc: Location? = null

    /**
     * Method use to get location
     * @return Location
     */
    private val location: Location?
        get() {
            try {
                locationManager = mContext
                    .getSystemService(LOCATION_SERVICE) as LocationManager

                // get GPS status
                checkGPS = locationManager!!
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)

                // get network provider status
                checkNetwork = locationManager!!
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                if (!checkGPS && !checkNetwork) {
                    Toast.makeText(mContext, "No Service Provider is available", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    canGetLocation = true

                    // if GPS Enabled get lat/long using GPS Services
                    if (checkGPS) {
                        if (ActivityCompat.checkSelfPermission(
                                mContext,
                                ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                mContext, ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            Toast.makeText(
                                mContext,
                                "Location Permission Is Not Granted!",
                                Toast.LENGTH_SHORT
                            ).show()
                            ActivityCompat.requestPermissions(
                                (mContext),
                                arrayOf(
                                    ACCESS_FINE_LOCATION,
                                    ACCESS_COARSE_LOCATION
                                ),
                                1234
                            )
                        }
                        locationManager!!.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                        )
                        if (locationManager != null) {
                            loc = locationManager!!
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            if (loc != null) {
                                latitude = loc!!.latitude
                                longitude = loc!!.longitude
                            }
                        }
                    }
                    if (checkNetwork) {
                        if (ActivityCompat.checkSelfPermission(
                                mContext,
                                ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                mContext, ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                        }
                        locationManager!!.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                        )
                        if (locationManager != null) {
                            loc = locationManager!!
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        }
                        if (loc != null) {
                            latitude = loc!!.latitude
                            longitude = loc!!.longitude
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return loc
        }

    fun getLongitude(): Double {
        if (loc != null) {
            longitude = loc!!.longitude
        }
        return longitude
    }

    fun getLatitude(): Double {
        if (loc != null) {
            latitude = loc!!.latitude
        }
        return latitude
    }

    /**
     * Use to Changing in setting
     *
     * @param context
     */
    fun showSettingsAlert(context: Context?) {
        val alertDialog = AlertDialog.Builder(
            mContext
        )
        alertDialog.setTitle("GPS is not Enabled!")
        alertDialog.setMessage("Do you want to turn on GPS?")
        alertDialog.setPositiveButton("Yes") { _, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            mContext.startActivity(intent)
        }
        alertDialog.setNegativeButton("No") { dialog, _ ->
            dialog.cancel()
            //                new AddSosDialog(context, AppPreference.getInstance(context).getUserData().getRwaid(), Double.toString(latitude), Double.toString(longitude)).show();
        }
        alertDialog.show()
    }

    /**
     * Stop Listener if not needed
     */
    fun stopListener() {
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(
                    mContext,
                    ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    mContext, ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            //   locationManager.removeUpdates(com.spideymanage.helper.LocationTrack.this);
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    //Runs when location is changed
    override fun onLocationChanged(location: Location) {
        loc!!.latitude = location.latitude
        loc!!.longitude = location.longitude
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
    override fun onProviderEnabled(s: String) {}
    override fun onProviderDisabled(s: String) {}

    companion object {
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 10
        private const val MIN_TIME_BW_UPDATES: Long = 60000
    }

    init {
        location
    }
}