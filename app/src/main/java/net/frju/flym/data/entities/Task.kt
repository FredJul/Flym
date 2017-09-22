package net.frju.flym.data.entities

import android.arch.persistence.room.Entity
import android.os.Parcel
import android.os.Parcelable
import paperparcel.PaperParcel

@PaperParcel
@Entity(tableName = "tasks", primaryKeys = arrayOf("entryId", "imageLinkToDl"))
data class Task(
        var entryId: String = "",
        var imageLinkToDl: String = "", // TODO try again with null when room will authorize it
        var numberAttempt: Int = 0) : Parcelable {

    companion object {
        @JvmField val CREATOR = PaperParcelTask.CREATOR
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        PaperParcelTask.writeToParcel(this, dest, flags)
    }
}
