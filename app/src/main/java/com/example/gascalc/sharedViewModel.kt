package com.example.gascalc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class sharedViewModel : ViewModel() {

    private val selectedItem : MutableLiveData<String> =  MutableLiveData<String>()
    private val gas : MutableLiveData<String> =  MutableLiveData<String>()

    public fun setMPG(item : String){
        selectedItem.value = item
    }

    public fun getMPG() : LiveData<String>{
        return selectedItem;
    }

    public fun setGasPrice(item : String){
        gas.value = item
    }

    public fun getGasPrice() : LiveData<String>{
        return gas;
    }

}