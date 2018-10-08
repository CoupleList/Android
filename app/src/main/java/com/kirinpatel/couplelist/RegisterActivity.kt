package com.kirinpatel.couplelist

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.support.v7.app.AppCompatActivity
import android.app.LoaderManager.LoaderCallbacks
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.support.design.widget.Snackbar
import android.text.TextUtils
import android.view.View
import android.widget.ArrayAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

import java.util.ArrayList

import kotlinx.android.synthetic.main.activity_register.*

/**
 * A login screen that offers login via email/password.
 */
class RegisterActivity : AppCompatActivity(), LoaderCallbacks<Cursor> {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserRegisterTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        register_button.setOnClickListener { attemptRegistration() }
    }

    private fun attemptRegistration() {
        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        username.error = null
        email.error = null
        password.error = null
        verify_password.error = null

        // Store values at the time of the login attempt.
        val usernameStr = username.text.toString()
        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()
        val verifyPasswordStr = verify_password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (TextUtils.isEmpty(usernameStr)) {
            username.error = "This field is required"
            focusView = username
            cancel = true
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(passwordStr)) {
            password.error = "This field is required"
            focusView = password
            cancel = true
        } else if (!isPasswordValid(passwordStr)) {
            password.error = "Your password must be at least 6 characters and include a number"
            focusView = password
            cancel = true
        }

        if (TextUtils.isEmpty(verifyPasswordStr)) {
            verify_password.error = "This field is required"
            focusView = password
            cancel = true
        } else if (passwordStr != verifyPasswordStr) {
            verify_password.error = "Provided passwords do not match"
            focusView = verify_password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            mAuthTask = UserRegisterTask(usernameStr, emailStr, passwordStr)
            mAuthTask!!.execute(null as Void?)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.contains("@")
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6 && password.contains(Regex("[0-9]"))
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            register_form.visibility = if (show) View.GONE else View.VISIBLE
            register_form.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 0 else 1).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            register_form.visibility = if (show) View.GONE else View.VISIBLE
                        }
                    })

            register_progress.visibility = if (show) View.VISIBLE else View.GONE
            register_progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 1 else 0).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            register_progress.visibility = if (show) View.VISIBLE else View.GONE
                        }
                    })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            register_progress.visibility = if (show) View.VISIBLE else View.GONE
            register_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        return CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE),

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC")
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        val emails = ArrayList<String>()
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS))
            cursor.moveToNext()
        }

        addEmailsToAutoComplete(emails)
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {

    }

    private fun addEmailsToAutoComplete(emailAddressCollection: List<String>) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        val adapter = ArrayAdapter(this@RegisterActivity,
                android.R.layout.simple_dropdown_item_1line, emailAddressCollection)

        email.setAdapter(adapter)
    }

    object ProfileQuery {
        val PROJECTION = arrayOf(
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY)
        val ADDRESS = 0
        val IS_PRIMARY = 1
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UserRegisterTask internal constructor(private val mUsername: String, private val mEmail: String, private val mPassword: String) : AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg params: Void): Void? {
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(mEmail, mPassword)
                    .addOnCompleteListener { task ->
                        mAuthTask = null
                        showProgress(false)

                        when {
                            task.isSuccessful -> {
                                val user = FirebaseAuth.getInstance().currentUser!!
                                FirebaseDatabase
                                        .getInstance()
                                        .reference
                                        .child("users")
                                        .child(user.uid)
                                        .child("username")
                                        .setValue(mUsername)
                                        .addOnCompleteListener { finish() }
                            }
                            task.isComplete -> {
                                Snackbar.make(
                                        root_layout,
                                        "Authentication failed. Email address may already be in use.",
                                        Snackbar.LENGTH_LONG)
                                        .show()
                            }
                        }
                    }

            return null
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }
    }
}
