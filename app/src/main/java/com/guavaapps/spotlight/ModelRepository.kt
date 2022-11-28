package com.guavaapps.spotlight

import com.guavaapps.spotlight.realm.Model

class ModelRepository(
    private val matcha: Matcha,
    private val remoteModelDataSource: ModelProvider,
) {
    private lateinit var userId: String

    lateinit var model: DLSTMPModel
        private set

    private lateinit var modelConfig: Model
    private lateinit var modelProvider: Match<Model>

    fun init() {
        userId = matcha.currentUser!!.customData["spotify_id"] as String

        val wrappedModel = loadModel()

        model = wrappedModel.model

        modelProvider = matcha.where(Model::class.java)
            .equalTo("_id", userId)

        modelConfig = modelProvider.findFirst() ?: Model(userId).apply { timestamp = 0L }
        watchModel()
    }

    fun watchModel() {
        modelProvider
            .watch {
                if (it.timestamp!! > modelConfig.timestamp!!) modelConfig = it
            }
    }

    fun close() {
        if (this::modelProvider.isInitialized) modelProvider.stopWatching()
    }

    fun optimiseModel(): ModelWrapper {
        return remoteModelDataSource.getOptimized(userId)
    }

    private fun loadModel(): ModelWrapper {
        return remoteModelDataSource.get(userId)
    }
}