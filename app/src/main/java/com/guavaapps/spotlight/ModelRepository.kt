package com.guavaapps.spotlight

import android.util.Log
import com.guavaapps.spotlight.realm.Model
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val TAG = "temp"

private const val FEATURES = 9

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

    suspend fun init(timeline: Array<FloatArray>) {
        userId = matcha.currentUser!!.customData["spotify_id"] as String

        modelConfigProvider = matcha.where(Model::class.java)
            .equalTo("_id", userId)

        withContext(Dispatchers.IO) {
            val modelConfig = modelConfigProvider.findFirst()

            if (modelConfig == null || !isConfigValid(modelConfig)) {
                model = optimiseModel(timeline).model!!
            } else {
                this@ModelRepository.modelConfig = modelConfig
                model = loadModel()?.model ?: optimiseModel(timeline).model!!
            }
        }

        watchModel()
    }

    fun watchModel() {
        modelConfigProvider
            .watch {
                if (it.timestamp!! > modelConfig.timestamp!!) {
                    modelConfig = it
                }
            }
    }

    fun close() {
        if (this::modelConfigProvider.isInitialized) {
            modelConfigProvider.stopWatching()
        }
    }

    suspend fun optimiseModel(timeline: Array<FloatArray>): ModelWrapper {
        val model = modelProvider.getOptimized(userId, timeline)
        val config = modelConfigProvider.findFirst()

        config?.let {
            withContext(Dispatchers.IO) {
                realm.executeTransaction {
                    it.insertOrUpdate(config)
                }
            }
        }

        return model
    }

    private suspend fun loadModel(): ModelWrapper? {
        // realm is main-thread initialised
        val localModelConfig = withContext(Dispatchers.IO) {
            realm.where(Model::class.java)
                .equalTo("_id", userId)
                .findFirstCopied()
        }

        val latestModelConfig =
            modelConfigProvider.findFirst()
                .also { Log.e("ModelRepository", "no model found") }

        val configNotNull = latestModelConfig != null

        if (!configNotNull) {
            return null
        }

        // debug
        val useLocal = true

        if (latestModelConfig?.timestamp == localModelConfig?.timestamp && useLocal) {
            val m = System.getProperty("java.io.tmpdir", ".")?.let {
                File(it)
                    .listFiles()
                    ?.maxBy { it.lastModified() }.also { m ->
                        File(it).listFiles()
                            ?.filterNot { it == m }
                            ?.forEach { it.delete() }
                    }
            }
            if (m != null) {
                Log.e(TAG, "timestamps match, using local copy")

                return ModelWrapper(
                    latestModelConfig?.version,
                    latestModelConfig?.timestamp,
                    DLSTMPModel(m)
                )
            }
        }

        Log.e(TAG, "timestamps do not match, requesting new copy")

        withContext(Dispatchers.IO) {
            realm.executeTransaction {
                it.insertOrUpdate(latestModelConfig)
            }
        }

        return modelProvider.get(userId).takeIf { it.verion == modelConfig.version }
    }

    private fun isConfigValid(config: Model) = config.model_params.last()?.shape?.last() == FEATURES
}