package net.frju.flym

import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

val FILE_SCHEME = "file://"
val UTF8 = "UTF-8"

fun String.toMd5(): String? {
    try {
        val md = MessageDigest.getInstance("MD5")
        val messageDigest = md.digest(toByteArray())
        val number = BigInteger(1, messageDigest)
        return number.toString(16)
    } catch (e: NoSuchAlgorithmException) {
        return null
    }

}