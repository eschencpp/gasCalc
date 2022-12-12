package com.example.gascalc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class DialogViewModel(val state : SavedStateHandle) : ViewModel() {

    private var startAddress : MutableLiveData<String> =  state.getLiveData("startAddr","Enter start location")
    private var destAddress : MutableLiveData<String> =  state.getLiveData("destAddr","Enter destination location")
    private var startLatitude : MutableLiveData<Double> =  state.getLiveData("startLat",0.0)
    private var startLong : MutableLiveData<Double> =  state.getLiveData("startLong",0.0)
    private var destLatitude : MutableLiveData<Double> =  state.getLiveData("destLat",0.0)
    private var destLong : MutableLiveData<Double> =  state.getLiveData("destLong",0.0)
    private var distance : MutableLiveData<Double> =  state.getLiveData("distance",0.0)
    private var gasPrice : MutableLiveData<String> =  state.getLiveData("gasPrice","")

    fun getStartAddr() : LiveData<String>{
        return startAddress;
    }

    fun setStartAddr(item : String){
        state.set("startAddr", item)
        startAddress.value = item
    }

    fun getdestAddr() : LiveData<String>{
        return destAddress;
    }

    fun setDestAddr(item : String){
        state.set("destAddr", item)
        destAddress.value = item
    }

    fun getStartLatitude() : LiveData<Double>{
        return startLatitude;
    }

    fun setStartLatitude(item : Double){
        state.set("startLat", item)
        startLatitude.value = item
    }

    fun getStartLong() : LiveData<Double>{
        return startLong;
    }

    fun setStartLong(item : Double){
        state.set("startLong", item)
        startLong.value = item
    }

    fun getDestLatitude() : LiveData<Double>{
        return destLatitude;
    }

    fun setDestLatitude(item : Double){
        state.set("destLat", item)
        destLatitude.value = item
    }

    fun getDestLong() : LiveData<Double>{
        return destLong;
    }

    fun setDestLong(item : Double){
        state.set("destLong", item)
        destLong.value = item
    }

    fun getDistance() : LiveData<Double>{
        return distance;
    }

    fun setDistance(item : Double){
        state.set("distance", item)
        distance.value = item
    }

    fun getGasPrice() : LiveData<String>{
        return gasPrice;
    }

    fun setGasPrice(item : String){
        state.set("gasPrice", item)
        gasPrice.value = item
    }

}