package com.github.livingwithhippos.unchained.plugins.model

import android.os.Parcel
import android.os.Parcelable

data class ScrapedItem(
    val name: String,
    val link: String?,
    val seeders: String? = null,
    val leechers: String? = null,
    val size: String? = null,
    val magnets: List<String>,
    val torrents: List<String>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createStringArrayList() ?: emptyList()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(link)
        parcel.writeString(seeders)
        parcel.writeString(leechers)
        parcel.writeString(size)
        parcel.writeStringList(magnets)
        parcel.writeStringList(torrents)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ScrapedItem> {
        override fun createFromParcel(parcel: Parcel): ScrapedItem {
            return ScrapedItem(parcel)
        }

        override fun newArray(size: Int): Array<ScrapedItem?> {
            return arrayOfNulls(size)
        }
    }
}