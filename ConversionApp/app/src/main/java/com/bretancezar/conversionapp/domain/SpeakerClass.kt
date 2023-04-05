package com.bretancezar.conversionapp.domain

enum class SpeakerClass {
    ORIGINAL {
        override fun toString(): String {
            return "original"
        }
             },
    A{
        override fun toString(): String {
            return "A"
        }
    },
    B{
        override fun toString(): String {
            return "B"
        }
    },
    C{
        override fun toString(): String {
            return "C"
        }
    }
}