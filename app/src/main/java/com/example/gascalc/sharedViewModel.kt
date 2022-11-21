package com.example.gascalc

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class sharedViewModel(val state : SavedStateHandle) : ViewModel() {

    private var MPG : MutableLiveData<String> =  state.getLiveData("mpg","")
    private var gas : MutableLiveData<String> =  state.getLiveData("gas","")

    fun setMPG(item : String){
        state.set("mpg", item)
        MPG.value = item
    }

    public fun getMPG() : LiveData<String>{
        return MPG;
    }

    public fun setGasPrice(item : String){
        state.set("gas", item)
        gas.value = item
    }

    public fun getGasPrice() : LiveData<String>{
        return gas;
    }

    fun saveState(){
        MPG.value = state["mpg"]
        gas.value = state["gas"]
    }

}