package com.guavaapps.spotlight

import android.app.Application
import io.realm.Realm

private const val APP = "spotlight-gnmnp"
private const val APP_NAME = "Spotlight"

private const val TAG = "Ky"

class Ky : Application() {
    lateinit var modelRepository: ModelRepository
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var localRealm: LocalRealm
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

        localRealm = LocalRealm(this)
    }

    override fun onTerminate() {
        super.onTerminate()

        modelRepository.close()
        matcha.logout()
        localRealm.close()
    }
}