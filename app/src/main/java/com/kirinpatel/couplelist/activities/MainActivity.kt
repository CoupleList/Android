package com.kirinpatel.couplelist.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.kirinpatel.couplelist.R
import kotlinx.android.synthetic.main.activity_register.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val usernameListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.exists()) {
                nav_view.getHeaderView(0).username_textView.text = dataSnapshot.value.toString()
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        nav_view.getHeaderView(0).email_textView.text = FirebaseAuth.getInstance().currentUser!!.email
    }

    override fun onResume() {
        super.onResume()

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            FirebaseDatabase
                    .getInstance()
                    .reference
                    .child("users")
                    .child(user.uid)
                    .child("username")
                    .removeEventListener(usernameListener)
        }
    }

    override fun onPause() {
        super.onPause()

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            FirebaseDatabase
                    .getInstance()
                    .reference
                    .child("users")
                    .child(user.uid)
                    .child("username")
                    .removeEventListener(usernameListener)
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                finish()
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_list -> {
                // Handle the camera action
            }
            R.id.nav_profile -> {

            }
            R.id.nav_share -> shareList()
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun shareList() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            FirebaseDatabase.getInstance().reference
                    .child("users")
                    .child(user.uid)
                    .child("list")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                generateShareableLink(
                                        dataSnapshot.child("key").value.toString(),
                                        dataSnapshot.child("code").value.toString())
                            } else {
                                Snackbar.make(root_layout,
                                        "Unable to create sharable link.",
                                        Snackbar.LENGTH_LONG)
                                        .show()
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {

                        }
                    })
        } else {
            Snackbar.make(root_layout,
                    "Unable to create sharable link.",
                    Snackbar.LENGTH_LONG)
                    .show()
        }
    }

    private fun generateShareableLink(key: String, code: String) {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://couplelist.app/?link=$key"))
                .setDynamicLinkDomain("sa6cz.app.goo.gl")
                .setAndroidParameters(DynamicLink.AndroidParameters.Builder().build())
                .setIosParameters(
                        DynamicLink.IosParameters.Builder("com.kirinpatel.couplelist")
                                .setAppStoreId("123456789")
                                .setMinimumVersion("1.0.1")
                                .build())
                .buildShortDynamicLink()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val extraText = "Hey, help me make our Couple List! Click this link below then use the password \"$code.\" " + task.result!!.shortLink
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, extraText)
                            type = "text/plain"
                        }
                        startActivity(sendIntent)
                    } else {
                        Snackbar.make(root_layout,
                                "Unable to create sharable link.",
                                Snackbar.LENGTH_LONG)
                                .show()
                    }
                }
    }
}
