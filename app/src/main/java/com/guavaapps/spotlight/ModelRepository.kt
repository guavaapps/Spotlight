package com.guavaapps.spotlight

import com.guavaapps.spotlight.realm.Model

class ModelRepository(
    private val matcha: Matcha,
    private val remoteModelDataSource: RemoteModelDataSource,
) {
    private lateinit var userId: String

    lateinit var model: DLSTMPModel
        private set

    private var modelConfig: Model? = null
    lateinit var modelProvider: Match<Model>

    fun init() {
        userId = matcha.currentUser!!.customData["spotify_id"] as String

        model = loadModel()

        modelProvider = matcha.where(Model::class.java)
            .equalTo("_id", userId)

        modelConfig = modelProvider.findFirst() ?: Model(userId).apply { timestamp = 0L }
        watchModel()
    }

    fun watchModel() {
        modelProvider
            .watch {
                if (it.timestamp!! > modelConfig!!.timestamp!!) modelConfig = it
            }
    }

    fun close() {
        modelProvider.stopWatching()
    }

    fun createModel(): DLSTMPModel {
        return remoteModelDataSource.createModel(userId)
    }

    private fun loadModel(): DLSTMPModel {
        return remoteModelDataSource.getModel(userId)
    }
}