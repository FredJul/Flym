package net.frju.flym.utils

import java.math.BigInteger
import java.security.MessageDigest

val FILE_SCHEME = "file://"
val UTF8 = "UTF-8"
val SHA1_INSTANCE: MessageDigest = MessageDigest.getInstance("SHA-1")

fun String.sha1(): String {
    val messageDigest = SHA1_INSTANCE.digest(toByteArray())
    val number = BigInteger(1, messageDigest)
    return number.toString(16)
}