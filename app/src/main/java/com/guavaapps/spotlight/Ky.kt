package com.guavaapps.spotlight

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

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

        val config = RealmConfiguration.Builder()
            .name("Spotlight")
            .allowQueriesOnUiThread(true)
            .allowWritesOnUiThread(true)
            .deleteRealmIfMigrationNeeded()
            .build()

        localRealm = Realm.getInstance(config)
        matcha = Matcha.init(applicationContext, APP, APP_NAME)

        val modelProvider = ModelProvider()

        modelRepository = ModelRepository(
            matcha,
            localRealm,
            modelProvider
        )

        userRepository = UserRepository(
            matcha
        )
    }

    override fun onTerminate() {
        super.onTerminate()

        modelRepository.close()
        matcha.logout()
        localRealm.close()
    }
}