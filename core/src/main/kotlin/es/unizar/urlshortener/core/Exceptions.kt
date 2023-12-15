package es.unizar.urlshortener.core

class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

class InvalidCustomWordException(customWord: String) : Exception("[$customWord] does not follow a supported schema")

class CustomWordInUseException(customWord: String) : Exception("[$customWord] is already in use")
