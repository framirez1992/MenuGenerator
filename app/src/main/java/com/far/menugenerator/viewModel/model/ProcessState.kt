package com.far.menugenerator.viewModel.model

data class ProcessState(val state: State, val message:String?=null, val code:Int=0)
enum class State{
    LOADING,
    SUCCESS,
    GENERAL_ERROR,
    NETWORK_ERROR
}