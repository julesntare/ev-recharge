package com.example.evrecharge

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class AuthActivity : AppCompatActivity() {
    lateinit var mGoogleSignInClient: GoogleSignInClient
    private val Req_Code: Int = 123
    private lateinit var  auth: FirebaseAuth
    private lateinit var loginBtn: TextView
    private lateinit var backToRegister: TextView
    private lateinit var googleSignInBtn: SignInButton
    var sharedPreferences: SharedPreferences? = null
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var db: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        FirebaseApp.initializeApp(this)

        db = FirebaseFirestore.getInstance()

        progressDialog = ProgressDialog(this)

        sharedPreferences = getSharedPreferences("EV_PREFERENCES", Context.MODE_PRIVATE)
        editor = sharedPreferences!!.edit()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        auth = FirebaseAuth.getInstance()
        loginBtn = findViewById(R.id.login_btn)
        backToRegister = findViewById(R.id.back_to_register)
        googleSignInBtn = findViewById(R.id.google_login_button)

        loginBtn.setOnClickListener { login() }
        backToRegister.setOnClickListener { goToRegister() }
        googleSignInBtn.setOnClickListener {
            Toast.makeText(this, "Logging In...", Toast.LENGTH_SHORT).show()
            signInGoogle()
        }
    }

    private fun signInGoogle() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, Req_Code)
    }

    // onActivityResult() function : this is where
    // we provide the task and data for the Google Account
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Req_Code) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        }
    }

    private fun handleResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                updateUI(account)
            }
            else {
                Toast.makeText(this, "Account not registered", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            println(e)
            Toast.makeText(this, "Something went wrong!!! Try Again.", Toast.LENGTH_SHORT).show()
        }
    }

    // this is where we update the UI after Google sign in takes place
    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isComplete) {
                val items = HashMap<String, Any>()
                items["firstname"] = account.displayName.toString()
                    .substring(0, account.displayName.toString().indexOf(" "))
                items["lastname"] = account.displayName.toString()
                    .substring(account.displayName.toString().indexOf(" ") + 1)
                items["mobile"] = "0780000000"
                items["email"] = account.email.toString()
                items["level"] = 3
                items["img"] = account.photoUrl.toString()

                db.collection("users").document(auth.currentUser!!.uid).set(items)
                    .addOnCompleteListener { task1 ->
                        if (task1.isSuccessful) {
                            progressDialog.dismiss()
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                            editor.putString("uid", task.result.user?.uid)
                            editor.putString("email", account.email.toString())
                            editor.putString("displayName", account.displayName.toString())
                            editor.putString("imagePath", account.photoUrl.toString())
                            editor.apply()
                            val intent = Intent(this, MapsActivityTest::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            progressDialog.dismiss()
                            Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            val intent = Intent(this, MapsActivityTest::class.java)
            startActivity(intent)
            finish()
        }

        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
            startActivity(
                Intent(
                    this, MapsActivityTest
                    ::class.java
                )
            )
            finish()
        }
    }

    private fun login(){
        progressDialog.setTitle("Authenticating...")
        progressDialog.show()
        val email = findViewById<EditText>(R.id.username_input)
        val password = findViewById<EditText>(R.id.pass)

        auth.signInWithEmailAndPassword(email.text.toString(),password.text.toString()).addOnCompleteListener { task ->
            if(task.isSuccessful){
//                retrieve user info from firestore document
                db.collection("users").document(task.result.user?.uid.toString()).get().addOnSuccessListener { documentSnapshot ->
                    editor.putString("uid", auth.currentUser?.uid)
                    editor.putString("email", documentSnapshot.getString("email"))
                    editor.putString("displayName", documentSnapshot.getString("firstname"))
                    editor.putString("imagePath", documentSnapshot.getString("imagePath"))
                    editor.apply()
                    progressDialog.hide()
                    val intent = Intent(this, MapsActivityTest::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }.addOnFailureListener { exception ->
            progressDialog.hide()
            Toast.makeText(applicationContext,exception.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun goToRegister(){
        val intent= Intent(this,SignupActivity::class.java)
        startActivity(intent)
    }
}