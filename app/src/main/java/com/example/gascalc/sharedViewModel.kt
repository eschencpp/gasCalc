package com.example.gascalc

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class sharedViewModel(val state : SavedStateHandle) : ViewModel() {

    private var MPG : MutableLiveData<String> =  state.getLiveData("mpg","")
    private var gas : MutableLiveData<String> =  state.getLiveData("gas","")
    private var firstRun : MutableLiveData<Boolean> =  state.getLiveData("firstRun",true)

    fun setMPG(item : String){
        state.set("mpg", item)
        MPG.value = item
    }

    fun getMPG() : LiveData<String>{
        return MPG;
    }

    fun setGasPrice(item : String){
        state.set("gas", item)
        gas.value = item
    }

    fun getGasPrice() : LiveData<String>{
        return gas
    }

    fun setFirstRun(item : Boolean){
        state.set("firstRun", item)
        firstRun.value = item
    }

    fun getFirstRun(): LiveData<Boolean>{
        return firstRun
    }

}