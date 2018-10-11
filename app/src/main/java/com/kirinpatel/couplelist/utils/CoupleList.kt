package com.kirinpatel.couplelist.utils

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.kirinpatel.couplelist.R

class CoupleList private constructor(private val key: String, private val code: String) {

    fun getKey(): String {
        return key
    }

    fun getCode(): String {
        return code
    }

    fun generateSharableLink(callback: (uri: Uri) -> Unit, error: () -> Unit) {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://couplelist.app/?link=$key"))
                .setDynamicLinkDomain("sa6cz.app.goo.gl")
                .setAndroidParameters(DynamicLink.AndroidParameters.Builder().build())
                .setIosParameters(
                        DynamicLink.IosParameters.Builder("com.kirinpatel.couplelist")
                                .setAppStoreId("1009116743")
                                .setMinimumVersion("2.0.0")
                                .build())
                .buildShortDynamicLink()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) callback(task.result!!.shortLink)
                    else error()
                }
    }

    companion object {
        private var instance: CoupleList? = null

        fun getInstance(activity: Activity): CoupleList {
            val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
            val key = sharedPref.getString(activity.getString(R.string.saved_list_key), "")
            val code = sharedPref.getString(activity.getString(R.string.saved_list_code), "")

            if (instance == null) {
                instance = CoupleList(key, code)
            }

            return instance as CoupleList
        }
    }
}