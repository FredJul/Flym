package net.frju.flym.data.entities

import android.os.Parcel
import android.os.Parcelable
import paperparcel.PaperParcel


@PaperParcel
class ItemWithFeed(var feedTitle: String? = null,
                   var feedLink: String = "",
                   var feedImageLink: String? = null,
                   var groupId: String? = null) : Item(), Parcelable {

    companion object {
        @JvmField val CREATOR = PaperParcelItemWithFeed.CREATOR
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        PaperParcelItemWithFeed.writeToParcel(this, dest, flags)
    }
}
