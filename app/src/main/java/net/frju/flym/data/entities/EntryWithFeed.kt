package net.frju.flym.data.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
class EntryWithFeed(var feedTitle: String? = null,
					var feedLink: String = "",
					var feedImageLink: String? = null,
					var groupId: String? = null) : Entry(), Parcelable