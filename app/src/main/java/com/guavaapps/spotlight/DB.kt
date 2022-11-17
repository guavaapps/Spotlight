//package com.guavaapps.spotlight
//
//import android.content.Context
//import android.util.Log
//import io.realm.Realm
//import io.realm.mongodb.App
//import io.realm.mongodb.AppConfiguration
//import io.realm.mongodb.Credentials
//import io.realm.mongodb.sync.*
//import org.bson.Document
//import org.bson.types.ObjectId
//
//class DB(val context: Context) {
//    private val TAG = "DB"
//    private val APP = "spotlight-kceqr"
//
//    private val USERS = "user_subscription"
//
//    private val SPOTIFY_ID = "spotify_id"
//
//    lateinit var app: App
//
//    fun login(user: String) {
//        Realm.init(context)
//
//        app = App(
//            AppConfiguration.Builder(APP)
//                .defaultSyncClientResetStrategy(object : DiscardUnsyncedChangesStrategy {
//                    override fun onBeforeReset(realm: Realm) {
//                        Log.e(TAG, "client reset onBeforeReset - realmPath=${realm.path}")
//                    }
//
//                    override fun onAfterReset(before: Realm, after: Realm) {
//                        Log.e(TAG, "client reset onAfterReset - beforePath=${before.path} afterPath=${after.path}")
//                    }
//
//                    override fun onError(session: SyncSession, error: ClientResetRequiredError) {
//                        Log.e(TAG, "client reset onError - error=${error.message}")
//                    }
//
//                })
//                .build()
//        )
//
//        val credentials = Credentials.customFunction(
//            Document(
//                mapOf(
//                    SPOTIFY_ID to user
//                )
//            )
//        )
//
//        app.loginAsync(credentials) {
//            if (it.isSuccess) {
//                val appUser = app.currentUser()!!
//
//                val userData = appUser.customData
//                Log.e(TAG, "userData: $userData")
//
//                val configuration = SyncConfiguration.Builder(appUser)
//                    .allowWritesOnUiThread(true)
//                    .allowQueriesOnUiThread(true)
//                    .initialSubscriptions { realm, subscriptions ->
////                        subscriptions.removeAll()
//                        subscriptions.addOrUpdate(
//                            Subscription.create(
//                                USERS,
//                                realm.where(User::class.java)
//                                    .equalTo(SPOTIFY_ID, user)
//                            )
//                        )
//                    }
//                    .build()
//
//                Realm.getInstanceAsync(configuration, object : Realm.Callback() {
//                    override fun onSuccess(realm: Realm) {
//                        Log.e(TAG, "realm instance obtained")
//
//                        realm.executeTransaction { r ->
//                            val userObject = r.createObject(User::class.java, ObjectId ())
//                            userObject.spotify_id = user
//
//                            val isNewUser = r.where(User::class.java)
//                                .count() == 0L
//
//                            if (isNewUser) r.insert(userObject)
//                        }
//                    }
//
//                    override fun onError(exception: Throwable) {
//                        Log.e(TAG, "realm sync error - ${exception.message}")
//                    }
//                })
//            } else {
//                Log.e(TAG, "${it.error.message}")
//            }
//        }
//    }
//}