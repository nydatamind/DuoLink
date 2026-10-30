package com.duolink.app.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = enumValueOf(value)

    @TypeConverter
    fun fromMessageStatus(value: MessageStatus): String = value.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = enumValueOf(value)

    @TypeConverter
    fun fromCallType(value: CallType): String = value.name

    @TypeConverter
    fun toCallType(value: String): CallType = enumValueOf(value)

    @TypeConverter
    fun fromCallDirection(value: CallDirection): String = value.name

    @TypeConverter
    fun toCallDirection(value: String): CallDirection = enumValueOf(value)

    @TypeConverter
    fun fromCallStatus(value: CallStatus): String = value.name

    @TypeConverter
    fun toCallStatus(value: String): CallStatus = enumValueOf(value)
}

