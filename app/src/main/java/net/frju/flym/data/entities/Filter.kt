/*
 * Copyright (c) 2012-2018 CodingSpiderFox
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.frju.flym.data.entities

import androidx.room.*
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.time.LocalDateTime

@Parcelize
@Entity(tableName = "filters",
		primaryKeys = ["filterId", "keywordToIgnore"],
		indices = [(Index(value = ["filterId"]))])
data class Filter(
		var filterId: String = "",
		var keywordToIgnore: String = "",
		var dateCreated: LocalDateTime = LocalDateTime.now()) : Parcelable
