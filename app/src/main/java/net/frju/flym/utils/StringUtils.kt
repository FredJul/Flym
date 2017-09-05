package net.frju.flym

import java.math.BigInteger
import java.security.MessageDigest

val FILE_SCHEME = "file://"
val UTF8 = "UTF-8"
val MD5_INSTANCE: MessageDigest = MessageDigest.getInstance("MD5")

fun String.toMd5(): String {
    val messageDigest = MD5_INSTANCE.digest(toByteArray())
    val number = BigInteger(1, messageDigest)
    return number.toString(16)
}