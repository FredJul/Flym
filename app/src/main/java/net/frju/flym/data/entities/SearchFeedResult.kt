package net.frju.flym.data.entities

import android.os.Parcel
import android.os.Parcelable
import ir.mirrajabi.searchdialog.core.Searchable
import paperparcel.PaperParcel

@PaperParcel
data class SearchFeedResult(
        var link: String = "",
        var name: String = "",
        var desc: String = "") : Parcelable, Searchable {

    override fun getTitle() = name

    companion object {
        @JvmField val CREATOR = PaperParcelSearchFeedResult.CREATOR
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        PaperParcelSearchFeedResult.writeToParcel(this, dest, flags)
    }
}
