package net.frju.flym.data.entities

import android.os.Parcel
import android.os.Parcelable
import paperparcel.PaperParcel


@PaperParcel
class EntryWithFeed(var feedTitle: String? = null,
                    var feedLink: String = "",
                    var feedImageLink: String? = null,
                    var groupId: String? = null) : Entry(), Parcelable {

    companion object {
        @JvmField
        val CREATOR = PaperParcelEntryWithFeed.CREATOR
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        PaperParcelEntryWithFeed.writeToParcel(this, dest, flags)
    }
}
