package com.example.evrecharge

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class DashboardActivity : AppCompatActivity() {
    private lateinit var sharedPreference: SharedPreferences
    private lateinit var firebaseDb: FirebaseFirestore
    private lateinit var editor: SharedPreferences.Editor
    private var toolbar: Toolbar? = null
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var actionBarToggle: ActionBarDrawerToggle
    private lateinit var profileName: TextView
    private lateinit var profileImage: ImageView
    private lateinit var recyclerview: RecyclerView
    private lateinit var adapter:CustomVisitedStationAdapter
    private lateinit var visitedStationList: ArrayList<VisitedStationsDataModel>
    private lateinit var userUID: String

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        mDrawerLayout = findViewById(R.id.drawer_layout)
        profileName = findViewById(R.id.profile_name)
        profileImage = findViewById(R.id.profile_image)

        actionBarToggle = ActionBarDrawerToggle(this, mDrawerLayout, 0, 0)
        mDrawerLayout.addDrawerListener(actionBarToggle)
        // Call syncState() on the action bar so it'll automatically change to the back button when the drawer layout is open
        actionBarToggle.syncState()

        firebaseDb = FirebaseFirestore.getInstance()

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // set item as selected to persist highlight
            menuItem.isChecked = true
            // close drawer when item is tapped
            mDrawerLayout.closeDrawers()
            // Handle navigation view item clicks here.
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MapsActivityTest::class.java)
                    startActivity(intent)
                }
                R.id.nav_logout -> {
                    MapsActivityTest().logout()
                }
            }
            // Add code here to update the UI based on the item selected
            // For example, swap UI fragments here

            true
        }
        navigationView.bringToFront()


        sharedPreference =  getSharedPreferences("EV_PREFERENCES", Context.MODE_PRIVATE)
        profileName.text = sharedPreference.getString("displayName", "")
        userUID = sharedPreference.getString("uid", "").toString()
        Glide.with(this).load(sharedPreference.getString("imagePath", "")).into(profileImage)

//        visited locations info
        recyclerview = findViewById(R.id.recyclerview)
        recyclerview.setHasFixedSize(true)
        recyclerview.layoutManager = LinearLayoutManager(this)
        visitedStationList = arrayListOf()

        // ArrayList of class ItemsViewModel
        val data = ArrayList<VisitedStationsDataModel>()

        firebaseDb = FirebaseFirestore.getInstance()
//        get visited stations where payment status equal status
        firebaseDb.collection("visited_stations")
            .whereIn("payment_status", listOf("SUCCESS", "PENDING"))
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    var stationName: String? = ""
                    var image_path:String? = ""
                    document.getDocumentReference("station_id")?.get()
                        ?.addOnSuccessListener { result ->
                            stationName = result.data?.get("name").toString()
                            image_path = result.data?.get("image_path").toString()
                        }
                    document.getDocumentReference("user_id")?.get()
                        ?.addOnSuccessListener { result ->
                            val userId = result.id
                            if (userId == userUID) {
                            val imagePath = image_path
                            val totalPaid = document.get("total_paid").toString().toInt()
                            val paymentMethod = document.getString("payment_method")
                            val visitedOn = document.getDate("visited_on")?.toLocaleString().toString()

                            val visitedStation = VisitedStationsDataModel(
                                stationName!!, userId,
                                imagePath!!, totalPaid, paymentMethod!!, visitedOn
                            )
                            data.add(visitedStation)
                            visitedStationList.addAll(data)

                            // This will pass the ArrayList to our Adapter
                            adapter = CustomVisitedStationAdapter(data)

                            // Setting the Adapter with the recyclerview
                            recyclerview.adapter = adapter
                                adapter.notifyDataSetChanged()
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                println("Error getting documents: $exception")
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
}
