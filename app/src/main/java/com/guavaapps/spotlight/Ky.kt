package com.guavaapps.spotlight

import android.app.Application
import org.bson.types.ObjectId

class Ky : Application() {
    lateinit var mongoClient: MongoClient
        private set

    lateinit var modelRepository: ModelRepository
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var localRealm: LocalRealm
        private set


    fun initForUser(userId: String) {
        mongoClient = MongoClient(this)

//        val remoteModelDataSource = RemoteModelDataSource()

//        modelRepository = ModelRepository(
//            userId,
//            mongoClient,
//            remoteModelDataSource
//        )

        userRepository = UserRepository(
            userId,
            mongoClient
        )

        localRealm = LocalRealm(this)
    }
}