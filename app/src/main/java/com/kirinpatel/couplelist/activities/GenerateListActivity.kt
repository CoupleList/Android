package com.kirinpatel.couplelist.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_generate_list.*
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.kirinpatel.couplelist.R

class GenerateListActivity : AppCompatActivity() {

    private lateinit var mDatabase: DatabaseReference
    private val listAccessListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            finish()
        }

        override fun onCancelled(databaseError: DatabaseError) {
            val user = FirebaseAuth.getInstance().currentUser

            mDatabase
                    .child("users")
                    .child(user!!.uid)
                    .child("list")
                    .removeValue()

            Snackbar.make(
                    root_layout,
                    "Unable to join list, please ensure the password you typed is correctly.",
                    Snackbar.LENGTH_LONG)
                    .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_list)

        mDatabase = FirebaseDatabase.getInstance().reference

        generate_list_button.setOnClickListener { showGenerateListDialog() }
        join_list_button.setOnClickListener {
            Snackbar.make(
                    root_layout,
                    "To join a list, ask your Partner for their list link, then, tap it!",
                    Snackbar.LENGTH_LONG)
                    .show()
        }
    }

    override fun onStart() {
        super.onStart()

        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(intent)
                .addOnSuccessListener(this) { pendingDynamicLinkData ->
                    if (pendingDynamicLinkData != null) {
                        val deepLink = pendingDynamicLinkData.link
                        val query = deepLink.encodedQuery
                        val key = query.substring(query.indexOf("link=") + 5, query.indexOf("&"))

                        joinList(key)
                    }
                }
                .addOnFailureListener {
                    Snackbar.make(
                            root_layout,
                            "Unable to open shared link! Please ask your Partner to send a new one.",
                            Snackbar.LENGTH_LONG)
                            .show()
                }
    }

    private fun showGenerateListDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val input = EditText(this)
        input.hint = "List Password"

        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setTitle("Set List Password")
        builder.setMessage("Secure your list with a known password." +
                "There are no requirements for this password, other than it must be provided.")
        builder.setView(input)

        builder.setPositiveButton("CREATE") { dialog, _ ->
            val password = input.text.toString()

            if (password.isEmpty()) {
                dialog.cancel()
                Snackbar.make(
                        root_layout,
                        "A password is required to create a list!",
                        Snackbar.LENGTH_LONG)
                        .setAction("OK") { showGenerateListDialog() }
                        .show()
            } else {
                generateList(password)
            }
        }
        builder.setNegativeButton("CANCEL", null)

        builder.show()
    }

    private fun joinList(key: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val input = EditText(this)
        input.hint = "List Password"

        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setTitle("Enter List Password")
        builder.setMessage("Please enter the password for your list.")
        builder.setView(input)

        builder.setPositiveButton("JOIN") { dialog, _ ->
            val password = input.text.toString()

            if (password.isEmpty()) {
                dialog.cancel()
                Snackbar.make(
                        root_layout,
                        "A password is required to join this list!",
                        Snackbar.LENGTH_LONG)
                        .setAction("OK") { joinList(key) }
                        .show()
            } else {
                val user = FirebaseAuth.getInstance().currentUser

                val listData: HashMap<String, Any> = HashMap()
                listData["key"] = key
                listData["code"] = password

                mDatabase
                        .child("users")
                        .child(user!!.uid)
                        .child("list")
                        .setValue(listData)
                        .addOnSuccessListener {
                            mDatabase
                                    .child("lists")
                                    .child(key)
                                    .child("activities")
                                    .addListenerForSingleValueEvent(listAccessListener)
                        }
            }
        }
        builder.setNegativeButton("CANCEL", null)

        builder.show()
    }

    private fun generateList(password: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val key = mDatabase.child("users").child(user!!.uid).push().key

        val listData: HashMap<String, Any> = HashMap()
        listData["key"] = key!!
        listData["code"] = password

        mDatabase
                .child("users")
                .child(user.uid)
                .child("list")
                .setValue(listData)
                .addOnSuccessListener {
                    mDatabase
                            .child("lists")
                            .child(key)
                            .child("code")
                            .setValue(password)
                            .addOnSuccessListener {
                                finish()
                            }
                            .addOnCanceledListener {
                                mDatabase
                                        .child("users")
                                        .child(user.uid)
                                        .child("list")
                                        .removeValue()
                                Snackbar.make(
                                        root_layout,
                                        "Unable to create a list, please try again.",
                                        Snackbar.LENGTH_LONG)
                                        .show()
                            }
                }
                .addOnFailureListener {
                    Snackbar.make(
                            root_layout,
                            "Unable to create a list, please try again.",
                            Snackbar.LENGTH_LONG)
                            .show()
                }
    }
}
