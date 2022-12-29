package com.guavaapps.spotlight

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

private const val APP = "spotlight-gnmnp"
private const val APP_NAME = "Spotlight"

private const val TAG = "Ky"

class Ky : Application() {
    lateinit var modelRepository: ModelRepository
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var localRealm: Realm
        private set

    lateinit var matcha: Matcha

    override fun onCreate() {
        super.onCreate()

        Realm.init(applicationContext)
        matcha = Matcha.init(applicationContext, APP, APP_NAME)

        val remoteModelDataSource = ModelProvider()

        modelRepository = ModelRepository(
            matcha,
            remoteModelDataSource
        )

        userRepository = UserRepository(
            matcha
        )

        val config = RealmConfiguration.Builder()
            .name("Spotlight")
            .allowQueriesOnUiThread(true)
            .allowWritesOnUiThread(true)
            .deleteRealmIfMigrationNeeded()
            .build()

        localRealm = Realm.getInstance(config)
    }

    override fun onTerminate() {
        super.onTerminate()

        modelRepository.close()
        matcha.logout()
        localRealm.close()
    }
}