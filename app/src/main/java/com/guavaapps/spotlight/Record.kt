package com.guavaapps.spotlight

import android.content.Context
import java.util.*

class Record private constructor (){
    private val objects: MutableList <Object> = mutableListOf()

    companion object {
        fun create (context: Context, objects: MutableList <Object>): Record {
            val record = Record ()
            record.objects =

            return record
        }
    }
}