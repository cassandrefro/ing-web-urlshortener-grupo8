package es.unizar.urlshortener.core

const val RETRY_AFTER = 5


class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

class UrlNotReachableException(url: String) : Exception("[$url] is not reachable")

class RedirectionNotReachableException(url: String) : Exception("[$url] is not reachable")

class InvalidCustomWordException(customWord: String) : Exception("[$customWord] does not follow a supported schema")

class CustomWordInUseException(customWord: String) : Exception("[$customWord] is already in use")

