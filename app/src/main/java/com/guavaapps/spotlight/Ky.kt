package com.guavaapps.spotlight

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val APP = "spotlight-gnmnp"
private const val APP_NAME = "Spotlight"

private const val TAG = "Ky"

class Ky : Application() {
    lateinit var modelRepository: ModelRepository
        private set

    lateinit var matcha: Matcha

    lateinit var realm: Realm

    override fun onCreate() {
        super.onCreate()

        Realm.init(applicationContext)

        val realmConfig = RealmConfiguration.Builder()
            .name("Spotlight")
            .allowQueriesOnUiThread(true)
            .allowWritesOnUiThread(true)
            .deleteRealmIfMigrationNeeded()
            .build()

        runBlocking(Dispatchers.IO) {
            // init on io thread
            realm = Realm.getInstance(realmConfig)
        }

        matcha = Matcha.init(applicationContext, APP, APP_NAME)

        val modelProvider = ModelProvider()

        modelRepository = ModelRepository(
            matcha,
            realm,
            modelProvider
        )
    }

    override fun onTerminate() {
        super.onTerminate()

        modelRepository.close()
        matcha.logout()
        realm.close()
    }
}