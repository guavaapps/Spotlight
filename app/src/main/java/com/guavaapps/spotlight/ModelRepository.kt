package com.guavaapps.spotlight

import android.util.Log
import com.guavaapps.spotlight.realm.Model
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ModelRepository(
    private val matcha: Matcha,
    private val realm: Realm,
    private val modelProvider: ModelProvider,
) {
    private lateinit var userId: String

    lateinit var model: DLSTMPModel
        private set

    private lateinit var modelConfig: Model
    private lateinit var modelConfigProvider: Match<Model>

    suspend fun init() {
        userId = matcha.currentUser!!.customData["spotify_id"] as String

        modelConfigProvider = matcha.where(Model::class.java)
            .equalTo("_id", userId)

        modelConfig = modelConfigProvider.findFirst() ?: Model(userId).apply { timestamp = 0L }

        val wrappedModel = loadModel()
        model = wrappedModel.model

        watchModel()
    }

    fun watchModel() {
        modelConfigProvider
            .watch {
                if (it.timestamp!! > modelConfig.timestamp!!) modelConfig = it
            }
    }

    fun close() {
        if (this::modelConfigProvider.isInitialized) modelConfigProvider.stopWatching()
    }

    fun optimiseModel(): ModelWrapper {
        return modelProvider.getOptimized(userId)
    }

    private suspend fun loadModel(): ModelWrapper {
        // realm is main-thread initialised
        val localModelConfig = withContext(Dispatchers.Main) {
            realm.where(Model::class.java)
                .equalTo("_id", userId)
                .findFirstCopied()
        }

        val latestModelConfig =
            modelConfigProvider.findFirst() ?: Model(userId).apply { timestamp = 0L }
                .also { Log.e("ModelRepository", "no model found") }

        val TAG = "temp"

        with(latestModelConfig) {
            Log.e(TAG, "MODEL CONFIG")
            Log.e(TAG, "    $spotify_id")
            Log.e(TAG, "    $timestamp")
            Log.e(TAG, "    $version")
        }

        if (latestModelConfig.timestamp == localModelConfig?.timestamp) {
            val m = System.getProperty("java.io.tmpdir", ".")?.let {
                File(it)
                    .listFiles()
                    ?.maxBy { it.lastModified() }
            }
            if (m != null) {
                Log.e(TAG, "timestamps match, using local copy")

                return ModelWrapper(latestModelConfig.version, latestModelConfig.timestamp, DLSTMPModel(m))
            }
        }

        Log.e(TAG, "timestamps do not match, requesting new copy")

        withContext(Dispatchers.Main) {
            realm.executeTransaction {
                it.insertOrUpdate(latestModelConfig)
            }
        }

        return modelProvider.get(userId)
    }
}