package com.kirinpatel.couplelist.activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.kirinpatel.couplelist.R

class LoadingActivity : AppCompatActivity() {

    private lateinit var mDatabase: DatabaseReference
    private var uid: String = ""

    private val userListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.exists()) {
                // User node exists in Firebase
                if (dataSnapshot.hasChild("list")) {
                    // Version 2 of list node exists in user node
                    startMainActivity()
                } else {
                    // Check if user has version 1 of list node in user node
                    var hasList = false

                    dataSnapshot.children.forEach { child ->
                        if (child.hasChild("code") && !child.hasChild("key")) {
                            // Version 1 of list node is in user node
                            hasList = true

                            upgradeListVersion(
                                    child.child("key").value.toString(),
                                    child.child("code").value.toString())
                        }
                    }

                    if (!hasList) {
                        startGenerateListActivity()
                    }
                }
            } else {
                // User node does no exist in Firebase
                startGenerateListActivity()
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            // TODO: Handle error
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        mDatabase = FirebaseDatabase.getInstance().reference
    }

    override fun onResume() {
        super.onResume()

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            startLoginActivity()
        } else {
            uid = user.uid
            mDatabase
                    .child("users")
                    .child(uid)
                    .addListenerForSingleValueEvent(userListener)
        }
    }

    override fun onPause() {
        super.onPause()

        if (uid.isNotEmpty()) {
            mDatabase
                    .child("users")
                    .child(uid)
                    .removeEventListener(userListener)

            uid = ""
        }
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun startGenerateListActivity() {
        val intent = Intent(this, GenerateListActivity::class.java)
        startActivity(intent)
    }

    private fun upgradeListVersion(key: String, code: String) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            // Set new list information
            val listData: HashMap<String, Any> = HashMap()
            listData["key"] = key
            listData["code"] = code
            mDatabase
                    .child("user")
                    .child(user.uid)
                    .child("list")
                    .setValue(listData)
                    .addOnSuccessListener {
                        // Remove old list information
                        mDatabase.child("user").child(user.uid).child(key).removeValue()
                    }
        }
    }
}
