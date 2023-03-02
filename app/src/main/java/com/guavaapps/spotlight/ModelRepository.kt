package com.guavaapps.spotlight

import android.util.Log
import com.guavaapps.spotlight.realm.Model
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val TAG = "temp"

private const val FEATURES = 9

// provides an constantly up to date dlstmp model
class ModelRepository(
    private val matcha: Matcha,
    private val realm: Realm,
    private val modelProvider: ModelProvider,
) {
    // spotify id of the current user
    private lateinit var userId: String

    lateinit var model: DlstmpModel
        private set

    // configuration document for the latest model
    private lateinit var modelConfig: Model

    // contains the watcher that triggers the model to update
    // when the remote configuration is changed
    private lateinit var modelConfigProvider: Match<Model>

    // initiate repository
    // provide a timeline in case the repository needs to build a new instance
    suspend fun init(timeline: Array<FloatArray>) {
        userId = matcha.currentUser!!.customData["spotify_id"] as String

        // create a config provider for the user's model config
        modelConfigProvider = matcha.where(Model::class.java)
            .equalTo("_id", userId)

        withContext(Dispatchers.IO) {
            // get the latest config
            val modelConfig = modelConfigProvider.findFirst()

            if (modelConfig == null || !isConfigValid(modelConfig)) {
                // build and optimize a new model if no config is found
                // or the config is invalid e.g. for a different version of the model
                model = optimiseModel(timeline).model!!
            } else {
                // load the model
                // if that fails, build new
                this@ModelRepository.modelConfig = modelConfig
                model = loadModel()?.model ?: optimiseModel(timeline).model!!
            }
        }

        //watchModel()
    }

    // TODO repurpose this functionality to listen for changes by other clients
    // TODO this would enable multi-device support
    @Deprecated("""
        The model config should be updated only after the model provider successfully returns
         a model so that in case the model provider fails to return 
         a model the local config will remain out of date
    """,
        level = DeprecationLevel.ERROR)
    fun watchModel() {
        modelConfigProvider
            .watch {
                if (it.timestamp!! > modelConfig.timestamp!!) {
                    modelConfig = it
                }
            }
    }

    // stop watcher
    fun close() {
        if (this::modelConfigProvider.isInitialized) {
            modelConfigProvider.stopWatching()
        }
    }

    // request an new optimized model from the model provider
    suspend fun optimiseModel(timeline: Array<FloatArray>): ModelWrapper {
        val model = modelProvider.getOptimized(userId, timeline)

        val config = modelConfigProvider.findFirst()

        config?.let {
            modelConfig = it
            withContext(Dispatchers.IO) {
                realm.executeTransaction {
                    it.insertOrUpdate(config)
                }
            }
        }

        return model
    }

    // load existing model instance
    private suspend fun loadModel(): ModelWrapper? {
        // get the local model config document
        // this describes the latest copy of the model
        val localModelConfig = withContext(Dispatchers.IO) {
            realm.where(Model::class.java)
                .equalTo("_id", userId)
                .findFirstCopied()
        }

        // get the latest model config from remote database
        val latestModelConfig =
            modelConfigProvider.findFirst()
                .also { Log.e("ModelRepository", "no model found") }

        //
        val configNotNull = latestModelConfig != null

        if (!configNotNull) {
            return null
        }

        // debug
        val useLocal = true

        if (latestModelConfig?.timestamp == localModelConfig?.timestamp && useLocal) {
            // if the local and latest timestamps match load the local model copy

            // get tf lite file from temp directory
            val m = System.getProperty("java.io.tmpdir", ".")?.let {
                File(it)
                    .listFiles()
                    ?.maxBy { it.lastModified() } // most recent
                    .also { m ->
                        File(it).listFiles()
                            ?.filterNot { it == m }
                            ?.forEach { it.delete() } // delete other files
                    }
            }

            if (m != null) {
                return ModelWrapper(
                    latestModelConfig?.version,
                    latestModelConfig?.timestamp,
                    DlstmpModel(m)
                )
            }
        }

        // if the timestamps do not match update the local config and
        // request a new model instance from the model provider
        return modelProvider.get(userId).takeIf { it.verion == modelConfig.version }.also {
            latestModelConfig?.let { modelConfig = it }

            withContext(Dispatchers.IO) {
                realm.executeTransaction {
                    it.insertOrUpdate(latestModelConfig)
                }
            }
        }
    }

    private fun isConfigValid(config: Model) = config.model_params.last()?.shape?.last() == FEATURES
}