package net.frju.flym.data.entities

import android.os.Parcelable
import ir.mirrajabi.searchdialog.core.Searchable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SearchFeedResult(
		var link: String = "",
		var name: String = "",
		var desc: String = "") : Parcelable, Searchable {

	override fun getTitle() = name

}
