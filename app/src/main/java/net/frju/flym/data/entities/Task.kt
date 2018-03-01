package net.frju.flym.data.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "tasks",
		primaryKeys = ["entryId", "imageLinkToDl"],
		indices = [(Index(value = ["entryId"]))],
		foreignKeys = [(ForeignKey(entity = Entry::class,
				parentColumns = ["id"],
				childColumns = ["entryId"],
				onDelete = ForeignKey.CASCADE))])
data class Task(
		var entryId: String = "",
		var imageLinkToDl: String = "", // TODO try again with null when room will authorize it
		var numberAttempt: Int = 0) : Parcelable
