package com.guavaapps.spotlight

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guavaapps.spotlight.Matcha.Companion.realmify
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.mongodb.Credentials
import org.bson.Document
import org.bson.types.ObjectId

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

        val remoteModelDataSource = RemoteModelDataSource()

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