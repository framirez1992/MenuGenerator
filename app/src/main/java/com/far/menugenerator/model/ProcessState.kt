package com.far.menugenerator.model

data class LoadingState(val state: State,val message:String?=null)
enum class State{
    LOADING,
    ERROR,
    SUCCESS
}