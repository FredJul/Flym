package net.frju.flym.utils

import okio.ByteString

val FILE_SCHEME = "file://"
val UTF8 = "UTF-8"

fun String.sha1(): String = ByteString.of(*this.toByteArray()).sha1().hex()