package com.example.evrecharge

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.evrecharge.BuildConfig.MAPS_API_KEY
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.mancj.materialsearchbar.MaterialSearchBar
import org.json.JSONObject
import java.util.*


open class MapsActivityTest : AppCompatActivity(), OnMapReadyCallback, LocationListener,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private lateinit var stationList: ArrayList<StationDataModel>
    private lateinit var firebaseDb: FirebaseFirestore
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var searchBar: MaterialSearchBar
    private lateinit var resultsContainer: LinearLayout
    companion object {
        private lateinit var mMap: GoogleMap
        private lateinit var mLastLocation: Location
        private lateinit var mLocationCallback: LocationCallback
        private lateinit var mGoogleApiClient: GoogleApiClient
        private lateinit var mLocationRequest: LocationRequest
        private lateinit var mFusedLocationClient: FusedLocationProviderClient
        private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
        var latitude: Double = 0.0
        private var longitude: Double = 0.0
        val path: MutableList<List<LatLng>> = ArrayList()
        lateinit var context:Context
        private lateinit var preferences: SharedPreferences
        private lateinit var editor: SharedPreferences.Editor
        private lateinit var evRechargeIcon: BitmapDescriptor
    }

    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var recyclerview:RecyclerView
    private lateinit var adapter:CustomAdapter
    private var loadIndicatorContainer: LinearProgressIndicator? = null
    var minDistanceMarker: Double = 180.0
    var iTracker: Int = -1

    private var mService: LocationUpdatesService? = null;
    // Tracks the bound state of the service.
    private var mBound: Boolean = false

    private val MY_PERMISSIONS_REQUEST_LOCATION = 68
    private val REQUEST_CHECK_SETTINGS = 129

    private var broadcastReceiver: BroadcastReceiver? = null

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MapsActivityTest, arrayOf(ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_LOCATION)
        } else {
            if (getLocationMode() == 3) {
                initializeService()
            } else {
                showAlertDialog(this@MapsActivityTest)
            }
        }
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {
                when (intent?.action) {
                    "NotifyUser" -> {
                        try {
                            val location: Location? = intent.getParcelableExtra("updatedLocation")

                            if (location != null) {
                                mLastLocation = location
                            }
                            if (mMap.isMyLocationEnabled) {
//                                mCurrLocationMarker.remove()
                                //Place current location marker
                                val latLng = LatLng(location!!.latitude, location.longitude)
                                val markerOptions = MarkerOptions()
                                latitude = latLng.latitude
                                longitude = latLng.longitude
                                markerOptions.position(latLng)
                                markerOptions.title("Your Current Address")
                                mMap.addMarker(markerOptions)!!

                                //move map camera
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                                mMap.animateCamera(CameraUpdateFactory.zoomTo(17f))
                            }

                            //stop location updates
                            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        val toolbar: Toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        context = applicationContext

        mDrawerLayout = findViewById(R.id.drawer_layout)

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // set item as selected to persist highlight
            menuItem.isChecked = true
            // close drawer when item is tapped
            mDrawerLayout.closeDrawers()

            // Handle navigation view item clicks here.
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_logout -> {
                    logout()
                }
            }
            // Add code here to update the UI based on the item selected
            // For example, swap UI fragments here

            true
        }
        navigationView.bringToFront()

        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

//        check if sharedPreference exists
        preferences = getSharedPreferences("EV_PREFERENCES", Context.MODE_PRIVATE)
        editor = preferences.edit()
        if(!preferences.contains("email")){
            if (GoogleSignIn.getLastSignedInAccount(this) != null) {
                mGoogleSignInClient.signOut()
            }
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
        }

        val color = ContextCompat.getColor(this, R.color.station_color)
//        evRechargeIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_baseline_ev_station_24)
        evRechargeIcon = BitmapHelper.vectorToBitmap(this, R.drawable.ic_baseline_ev_station_24, color)

        loadIndicatorContainer = findViewById(R.id.loadIndicator)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = 0

        searchBar = findViewById(R.id.searchBar)
        resultsContainer = findViewById(R.id.search_result)

        searchBar.addTextChangeListener(object:TextWatcher {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!searchBar.isSearchOpened) {
                    resultsContainer.visibility = View.GONE
                }
                else {
                    resultsContainer.visibility = View.VISIBLE
                }
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                loadStations()
            }

            override fun afterTextChanged(editable: Editable?) {
                filter(editable.toString())
            }
        })

//        locations info
        recyclerview = findViewById(R.id.recyclerview)
        recyclerview.setHasFixedSize(true)
        recyclerview.layoutManager = LinearLayoutManager(this)
        stationList = arrayListOf()
        adapter = CustomAdapter(stationList, this, false)
        recyclerview.adapter = adapter

        // ArrayList of class ItemsViewModel
        val data = ArrayList<StationDataModel>()
        var counter = 0

        firebaseDb = FirebaseFirestore.getInstance()

//        load callback_url collection
        firebaseDb.collection("callback_url").get()
            .addOnSuccessListener { result ->
                for (document in result) {
//                    save to shared preference
                    editor.putString("callback_url", document.data["cUrl"].toString())
                    editor.apply()
                }
            }
            .addOnFailureListener { exception ->
                Log.d("TAG", "Error getting documents: ", exception)
            }

        firebaseDb.collection("stations").get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val stationId = counter
                    counter += 1
                    val stationRefId = document.id
                    val name = document.getString("name")
                    val address = document.getString("address")
                    val imagePath = document.getString("image_path")
                    val mobileNo = document.getString("mobile_no") ?: "0788888888"
                    val latLng = LatLng(
                        document.getGeoPoint("location")?.latitude ?: 0.0,
                        document.getGeoPoint("location")?.longitude ?: 0.0
                    )
                    val station = StationDataModel(stationId, stationRefId, name!!, latLng, address!!, imagePath!!, mobileNo!!)
                    data.add(station)
                }
                stationList.addAll(data)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                println("Error getting documents: $exception")
            }

        // This will pass the ArrayList to our Adapter
        adapter = CustomAdapter(data, this)

        // Setting the Adapter with the recyclerview
        recyclerview.adapter = adapter
    }

    fun logout() {
        editor.clear()
        editor.apply()
//        kotlin android google sign out
        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
//            sign out if task is successful
            mGoogleSignInClient.signOut()
                .addOnCompleteListener(this) {
                    if (it.isComplete) {
                        val intent = Intent(this, AuthActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mDrawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {
        firebaseDb.collection("stations").get()
            .addOnSuccessListener {
                mMap = googleMap
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(this,
                            ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        buildGoogleApiClient()
                        mMap.isMyLocationEnabled = true
                    }
                } else {
                    buildGoogleApiClient()
                    mMap.isMyLocationEnabled = true
                }
                latitude = CurrentLocationAccess(this).getLatitude()
                longitude = CurrentLocationAccess(this).getLongitude()
                // Add a marker in current location standing and move the camera
                val currentLocation = LatLng(latitude, longitude)
                mMap.addMarker(MarkerOptions().position(currentLocation).title("Your current location"))
                stationList.forEach { place ->
                    run {
                        val distance: Double = SphericalUtil.computeDistanceBetween(
                            LatLng(
                                place.latLng.latitude, place.latLng.longitude
                            ), LatLng(latitude, longitude)
                        )
                        val distanceInKm = distance / 1000
                        if (minDistanceMarker > distanceInKm) {
                            minDistanceMarker = distanceInKm
                            iTracker += 1
                        }
                        mMap.addMarker(
                            MarkerOptions()
                                .title(place.name as String?)
                                .position(place.latLng)
                                .icon(evRechargeIcon)
                                .apply { snippet(place.address) }
                        )
                    }
                }

//        pass station list through sharedPreference
                val sharedPref = getSharedPreferences("stationList", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                val gson = Gson()
                val json = gson.toJson(stationList)
                editor.putString("stationList", json)
                editor.apply()

                mMap.setOnMarkerClickListener { marker -> // on marker click we are getting the title of our marker
                    println(marker.position)
                    val distance: Double = SphericalUtil.computeDistanceBetween(
                        LatLng(
                            marker.position.latitude, marker.position.longitude
                        ), LatLng(latitude, longitude)
                    )
                    if (marker.title != "Your current location") {
                        val builder = AlertDialog.Builder(this@MapsActivityTest)
                        builder.setTitle(marker.title)
                        builder.setMessage(
                            "${marker.snippet}\nDistance: ${
                                String.format(
                                    "%.2f",
                                    distance / 1000
                                )
                            } km"
                        )
                        builder.show()
                    }
                    false
                }
                if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    println(stationList.size)
                    updateRoute(stationList[iTracker], this.applicationContext)
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
                mMap.animateCamera(CameraUpdateFactory.zoomIn())
                mMap.animateCamera(CameraUpdateFactory.zoomTo(17f), 2000, null)
            }
    }

    private fun updateRoute(obj: StationDataModel, context: Context) {
        val urlDirections = "https://maps.googleapis.com/maps/api/directions/json?origin=$latitude,$longitude" +
                "&destination=${obj.latLng.latitude},${obj.latLng.longitude}&key=${MAPS_API_KEY}"
        val directionsRequest = @RequiresApi(Build.VERSION_CODES.TIRAMISU)

        object : StringRequest(Method.GET, urlDirections, Response.Listener {
                response ->
            val jsonResponse = JSONObject(response)
            // Get routes
            val routes = jsonResponse.getJSONArray("routes")
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val steps = legs.getJSONObject(0).getJSONArray("steps")
            val prevPathSize = path.size
            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                path.add(PolyUtil.decode(points))
            }

            mMap.moveCamera(CameraUpdateFactory.newLatLng(obj.latLng))
            mMap.clear()
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(destLatLng))
//            loop through the path and add the poly-lines
            for (i in prevPathSize until path.size) {
                mMap.addPolyline(PolylineOptions().addAll(path[i]).color(Color.RED).width(10f))
                mMap.addMarker(
                    MarkerOptions()
                        .title(obj.name)
                        .position(obj.latLng)
                        .icon(evRechargeIcon)
                        .apply { snippet(obj.address) }
                )
                loadIndicatorContainer?.hide()
            }
        }, Response.ErrorListener {
        }){}
        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(directionsRequest)
    }

    fun getPosition(obj: StationDataModel, context: Context) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = 150
        updateRoute(obj, context)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadStations() {
        recyclerview = findViewById(R.id.results_list)
        recyclerview.setHasFixedSize(true)
        recyclerview.layoutManager = LinearLayoutManager(this)

        // This will pass the ArrayList to our Adapter
        adapter = CustomAdapter(stationList, this, false)

        // Setting the Adapter with the recyclerview
        recyclerview.adapter = adapter
    }

    private fun filter(text: String) {
        //new array list that will hold the filtered data
        val filteredNames = ArrayList < StationDataModel > ()
        //looping through existing elements and adding the element to filtered list
        stationList.filterTo(filteredNames) {
            //if the existing elements contains the search input
            it.name.lowercase(Locale.ROOT).contains(text.lowercase(Locale.ROOT))
        }
        //calling a method of the adapter class and passing the filtered list
        adapter.filterList(filteredNames)
    }

    @Synchronized
    protected fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API).build()
        mGoogleApiClient.connect()
    }

    override fun onConnected(p0: Bundle?) {
        mLocationRequest = LocationRequest()
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    latitude = location.latitude
                    longitude = location.longitude
                }
            }
        }
        mLocationRequest.interval = 10000
        mLocationRequest.fastestInterval = 1000
        mLocationRequest.priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
        if (ContextCompat.checkSelfPermission(this,
                ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        }
    }

    override fun onConnectionSuspended(p0: Int) {
        MaterialAlertDialogBuilder(this@MapsActivityTest)
            .setTitle("Connection Status")
            .setMessage("Connection Suspended")
            .setPositiveButton(
                "RETRY"
            ) { _, _ -> }
            .setNegativeButton(
                "CANCEL"
            ) { _, _ -> }
            .show()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        MaterialAlertDialogBuilder(this@MapsActivityTest)
            .setTitle("Connection Status")
            .setMessage("Connection Failed!!!")
            .setPositiveButton(
                "RETRY"
            ) { _, _ -> }
            .setNegativeButton(
                "CANCEL"
            ) { _, _ -> }
            .show()
    }

    override fun onLocationChanged(location: Location) {}

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction("NotifyUser")
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(this).registerReceiver(it, intentFilter)
        }
    }

    override fun onPause() {
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
        }
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.e("MainActivity:","Location Permission Granted")
                    if (getLocationMode() == 3) {
                        Log.e("MainActivity:","Already set High Accuracy Mode")
                        initializeService()
                    } else {
                        Log.e("MainActivity:","Alert Dialog Shown")
                        showAlertDialog(this@MapsActivityTest)
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
        }
    }

    private fun showAlertDialog(context: Context?) {
        try {
            context?.let {
                val builder = AlertDialog.Builder(it)
                builder.setTitle(it.resources.getString(R.string.app_name))
                    .setMessage("Please select High accuracy Location Mode from Mode Settings")
                    .setPositiveButton(it.resources.getString(android.R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                        startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_CHECK_SETTINGS)
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLocationMode(): Int {
        return Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            initializeService()
        }
    }

    // Monitors the state of the connection to the service.
    private var mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder: LocationUpdatesService.LocalBinder = service as LocationUpdatesService.LocalBinder
            mService = binder.service
            mBound = true
            // Check that the user hasn't revoked permissions by going to Settings.

            mService?.requestLocationUpdates()

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
            mBound = false
        }
    }

    private fun initializeService(){
        bindService(Intent(this, LocationUpdatesService::class.java), mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection)
            mBound = false
        }
        super.onStop()
    }
}