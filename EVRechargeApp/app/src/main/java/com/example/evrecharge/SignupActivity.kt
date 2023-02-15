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
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {
    private lateinit var  auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var registerBtn:TextView
    private lateinit var backToLogin: TextView
    private lateinit var googleRegisterBtn: SignInButton
    private lateinit var fullName: EditText
    private lateinit var mobileNo: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    var sharedPreferences: SharedPreferences? = null
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth= FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        db = FirebaseFirestore.getInstance()
        registerBtn = findViewById(R.id.register_btn)
        backToLogin = findViewById(R.id.back_to_login)
        googleRegisterBtn = findViewById(R.id.google_register_button)

        sharedPreferences = getSharedPreferences("EV_PREFERENCES", Context.MODE_PRIVATE)
        editor = sharedPreferences!!.edit()

//        signup inputs
        fullName = findViewById(R.id.fullname_input)
        mobileNo = findViewById(R.id.mobileno_input)
        email = findViewById(R.id.email_input)
        password = findViewById(R.id.password_input)
        confirmPassword = findViewById(R.id.confirm_password_input)

        registerBtn.setOnClickListener { register() }
        backToLogin.setOnClickListener { goToLogin() }
        googleRegisterBtn.setOnClickListener { registerGoogle() }
    }

    private fun registerGoogle() {
//        kotlin google signup firebase
    }

    private fun register(){
        if(fullName.text.toString().isEmpty() || mobileNo.text.toString().isEmpty() ||
            email.text.toString().isEmpty() || password.text.toString().isEmpty() ||
            confirmPassword.text.toString().isEmpty()){
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.text.toString() != confirmPassword.text.toString()) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.text.length < 6) {
            Toast.makeText(this, "Passwords Characters must be above 6", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.setTitle("Processing Sign Up...")
        progressDialog.show()
        auth.createUserWithEmailAndPassword(email.text.toString(),password.text.toString()).addOnCompleteListener { task ->
            if(task.isSuccessful){
                val items = HashMap<String, Any>()
                items["firstname"] = fullName.text.toString().substring(0, fullName.text.toString().indexOf(" "))
                items["lastname"] = fullName.text.toString().substring(fullName.text.toString().indexOf(" ")+1)
                items["mobile"] = mobileNo.text.toString()
                items["email"] = email.text.toString()
                items["level"] = 3
                items["img"] = ""

                db.collection("users").document(auth.currentUser!!.uid).set(items).addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        Toast.makeText(this, "User Created Successfully", Toast.LENGTH_SHORT).show()

                        editor.putString("uid", auth.currentUser!!.uid)
                        editor.putString("email", email.text.toString())
                        editor.putString("displayName", fullName.text.toString())
                        editor.putString("imagePath", "")
                        editor.apply()
    progressDialog.hide()
                        val intent= Intent(this,MapsActivityTest::class.java)
                        startActivity(intent)
                        finish()
                    }else{
                        progressDialog.hide()
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.addOnFailureListener {
            progressDialog.hide()
            Toast.makeText(applicationContext, "Something went wrong. Try Again",Toast.LENGTH_LONG).show()
        }
    }

    private fun goToLogin(){
        val intent= Intent(this,AuthActivity::class.java)
        startActivity(intent)
    }
}