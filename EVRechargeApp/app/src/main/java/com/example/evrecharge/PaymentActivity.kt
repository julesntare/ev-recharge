package com.example.evrecharge

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import org.json.JSONObject

class PaymentActivity : AppCompatActivity() {
    private lateinit var firebaseDb: FirebaseFirestore
    private var toolbar: Toolbar? = null
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var actionBarToggle: ActionBarDrawerToggle
    private lateinit var paymentAccountNumber: EditText
    private lateinit var paymentAmount: EditText
    private lateinit var paymentMethod: Spinner
    private lateinit var paymentButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var progressDialog: ProgressDialog

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        mDrawerLayout = findViewById(R.id.drawer_layout)

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

//        load stationList json from shared preferences
        sharedPreferences = getSharedPreferences("stationList", MODE_PRIVATE)
        val stationListJson = sharedPreferences.getString("stationList", null)
        val stationList = Gson().fromJson(stationListJson, Array<StationDataModel>::class.java).toList()

//        get intent stationId extra
        val stationId = intent.getIntExtra("stationId", 0)

//        get uid from EV_PREFERENCES
        val uid = getSharedPreferences("EV_PREFERENCES", MODE_PRIVATE).getString("uid", null)

        progressDialog = ProgressDialog(this)

        paymentAccountNumber = findViewById(R.id.account_number)
//        set paymentAccountNumber to shared preferences account number if exists
        val accountNo = getSharedPreferences("EV_PREFERENCES", MODE_PRIVATE).getString("accountNumber", null)
        if (accountNo != null) {
            paymentAccountNumber.setText(accountNo)
        }
        paymentAmount = findViewById(R.id.payment_amount)
        paymentAmount.setText("0")

        paymentMethod = findViewById(R.id.payment_method)
        paymentButton = findViewById(R.id.pay_btn)

//                generate random uuid
        val uuid = java.util.UUID.randomUUID().toString()

//        get payment methods from array layout
        val paymentMethodList = resources.getStringArray(R.array.payment_method)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, paymentMethodList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        paymentMethod.adapter = adapter
        paymentButton.setOnClickListener {
            val accountNumber = paymentAccountNumber.text.toString()
            val amount = paymentAmount.text.toString()
            val method = paymentMethod.selectedItem.toString()

            if ((amount == "") || (amount.toInt() < 1)) {
                paymentAmount.error = "0 amount not allowed"
            }
            else if (accountNumber == "") {
                paymentAccountNumber.error = "Must add your Account No."
            }
            else {
//                set phone number to shared preferences
                val editor = getSharedPreferences("EV_PREFERENCES", MODE_PRIVATE).edit()
                editor.putString("accountNumber", accountNumber)
                editor.apply()

//                get cUrl from shared preference
                val cUrl = getSharedPreferences("EV_PREFERENCES", MODE_PRIVATE).getString("callback_url", null)

                progressDialog.setTitle("Processing Payment...")
                progressDialog.show()
//                send volley requestQueue to payment API
                val requestQueue = Volley.newRequestQueue(this)
                val url = "https://opay-api.oltranz.com/opay/paymentrequest"
                val jsonBody = JSONObject()
                jsonBody.put("telephoneNumber", accountNumber)
                jsonBody.put("amount", amount.toDouble())
                jsonBody.put("organizationId", "4800ecf9-585f-4009-88b7-fd6bf35acdfd")
                jsonBody.put("callbackUrl", cUrl)
                jsonBody.put("description", "Pay to ${stationList[stationId].name} for EV Recharge")
                jsonBody.put("transactionId", uuid)
                val jsonObjectRequest = JsonObjectRequest(
                    Request.Method.POST, url, jsonBody,
                    { response ->
                        progressDialog.hide()
                        println(response)
//                        convert response to json object to get transactionId from body object
                        val responseJson = response.toString()
                        val responseJsonObject = JSONObject(responseJson)
                        val status = responseJsonObject.getString("status")
                        val body = responseJsonObject.getJSONObject("body")
                        val transactionId = body.getString("transactionId")

//                        save transactionId and status to firebase
//                        save stationId to firebase as document reference
                        val stationRef = firebaseDb.collection("stations").document(stationList[stationId].stationRefId)
                        val userRef = firebaseDb.collection("users").document(uid.toString())
                        val transactionData = hashMapOf(
                            "transaction_id" to transactionId,
                            "payment_status" to status,
                            "station_id" to stationRef,
                            "total_paid" to amount.toInt(),
                            "payment_method" to method,
                            "account_no" to accountNumber,
                            "user_id" to userRef,
                            "visited_on" to FieldValue.serverTimestamp()
                        )
                        firebaseDb.collection("visited_stations").document(uuid).set(transactionData)
                            .addOnSuccessListener {
//                                show success message
                                Toast.makeText(this, "Proceed to confirm payment request", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, DashboardActivity::class.java)
                                startActivity(intent)
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Payment Request Failed", Toast.LENGTH_SHORT).show()
                            }
                    },
                    {
                        progressDialog.hide()
                        Toast.makeText(this, "Payment Request Failed", Toast.LENGTH_SHORT).show()
                    }
                )
                requestQueue.add(jsonObjectRequest)
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
}