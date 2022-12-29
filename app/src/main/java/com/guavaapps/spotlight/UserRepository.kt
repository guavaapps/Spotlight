package com.guavaapps.spotlight

import com.guavaapps.spotlight.realm.AppUser
import io.realm.mongodb.App

class UserRepository(
    private val matcha: Matcha,
) {
    private var userId: String? = null

    fun get(): AppUser {
        userId = userId ?: matcha.currentUser!!.customData["spotify_id"] as String

        val user = matcha.where(AppUser::class.java)
            .equalTo("_id", userId!!)
            .findFirst()!!

        return user
    }

    fun update(user: AppUser) {
        userId = userId ?: matcha.currentUser!!.customData["spotify_id"] as String

        matcha.where(AppUser::class.java)
            .equalTo("_id", userId!!)
            .update(user)
    }
}