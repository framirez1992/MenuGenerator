package com.far.menugenerator.model

data class ProcessState(val state: State, val message:String?=null)
enum class State{
    LOADING,
    SUCCESS,
    GENERAL_ERROR,
    NETWORK_ERROR
}