package net.frju.flym.data

import android.os.Parcel
import android.os.Parcelable
import net.frju.androidquery.annotation.DbField
import net.frju.androidquery.annotation.DbModel
import net.frju.flym.data.db.LocalDatabaseProvider
import paperparcel.PaperParcel

@PaperParcel
@DbModel(databaseProvider = LocalDatabaseProvider::class)
class Task : Parcelable {

    @DbField(primaryKey = true, autoIncrement = true)
    var id = 0 // TODO only to be able to use model() methods, should support multiple primary keys

    @DbField(unique = true, uniqueGroup = 1)
    var itemId = ""

    @DbField(unique = true, uniqueGroup = 1)
    var imageLinkToDl = ""

    @DbField
    var numberAttempt = 0

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        PaperParcelTask.writeToParcel(this, dest, flags)
    }

    companion object {
        @JvmField val CREATOR = PaperParcelTask.CREATOR
    }
}
