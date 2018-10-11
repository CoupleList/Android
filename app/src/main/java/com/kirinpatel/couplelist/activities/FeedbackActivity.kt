package com.kirinpatel.couplelist.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.kirinpatel.couplelist.R
import kotlinx.android.synthetic.main.activity_feedback.*

class FeedbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        submit.setOnClickListener { submitFeedback() }
    }

    private fun submitFeedback() {
        val strFeedback = feedback.text.toString()

        if (strFeedback.isEmpty()) {
            Snackbar.make(
                    root_layout,
                    "You cannot submit blank feedback.",
                    Snackbar.LENGTH_LONG)
                    .show()
        } else {
            val user = FirebaseAuth.getInstance().currentUser!!
            val key = FirebaseDatabase.getInstance().reference
                    .child("feedback")
                    .child(user.uid)
                    .push()
                    .key!!
            val data: HashMap<String, Any> = HashMap()
            data["feedback"] = strFeedback
            data["respondViaEmail"] = false
            FirebaseDatabase.getInstance().reference
                    .child("feedback")
                    .child(user.uid)
                    .child(key)
                    .setValue(data)

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)

            builder.setTitle("Thank you!")
            builder.setMessage("Your feedback will be reviewed and addressed asap.")

            builder.setNeutralButton("CLOSE", null)
            builder.setOnDismissListener { finish() }

            builder.show()
        }
    }
}
