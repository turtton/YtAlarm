package net.turtton.ytalarm.util.extensions

fun List<String>.joinStringWithSlash(): String {
    return joinToString(separator = "/")
}