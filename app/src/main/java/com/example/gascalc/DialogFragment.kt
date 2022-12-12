package com.example.gascalc

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.gascalc.databinding.FragmentBottomBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DialogFragment : DialogFragment() {
    private lateinit var viewmodel : DialogViewModel
    private var _binding: FragmentBottomBinding? = null
    private val binding get() = _binding!!
    private lateinit var calc_button : Button
    lateinit var start_loc_text : EditText
    lateinit var end_loc_text : EditText
    lateinit var distanceT : TextView
    lateinit var costT : TextView

    private var startAddress : String = "Your Location"
    private var destAddress : String = "Destination"
    private var startLatitude : Double? = null
    private var startLongitude : Double? = null
    private var destLatitude : Double? = null
    private var destLongitude : Double? = null

    //Setup UI
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setContentView(R.layout.fragment_bottom)
        setDialog()
        start_loc_text.setText(viewmodel.getStartAddr().value)
        end_loc_text.setText(viewmodel.getdestAddr().value)
        dialog?.show()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.getAttributes()?.windowAnimations = R.style.DialogAnimation
        dialog?.window?.setGravity(Gravity.BOTTOM)
        return binding.root
    }

    //Ensure that location names prevail on rotation
    override fun onResume() {
        super.onResume()
        start_loc_text.setText(viewmodel.getStartAddr().value)
        end_loc_text.setText(viewmodel.getdestAddr().value)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentBottomBinding.inflate(layoutInflater)
        viewmodel = ViewModelProvider(requireActivity()).get(DialogViewModel::class.java)
        return super.onCreateDialog(savedInstanceState)
    }


    fun setDialog(){
        calc_button = binding.calButton
        start_loc_text = binding.editTextStartLocation
        end_loc_text  = binding.editTextEndLocation
        distanceT  = binding.distanceText
        costT = binding.costText

        start_loc_text.setOnFocusChangeListener{ _, hasFocus ->
            if (hasFocus)
                start_loc_text.setText("")
            else
                start_loc_text.setText(viewmodel.getStartAddr().value)
        }

        start_loc_text.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_NEXT){
                viewmodel.setStartAddr(start_loc_text.text.toString())
                startAddress = viewmodel.getStartAddr().value!!
                start_loc_text.setText(startAddress)
                lifecycleScope.launch{
                    (activity as MapsActivity).toLatLng(start_loc_text.text.toString(),0)
                    startAddress = viewmodel.getStartAddr().value!!
                    delay(1000L)
                    start_loc_text.setText(startAddress)
                }
                true
            } else {
                false
            }
        }


        end_loc_text.setOnFocusChangeListener(){_, hasFocus ->
            if (hasFocus)
                end_loc_text.setText("")
            else
                end_loc_text.setText(viewmodel.getdestAddr().value)
        }

        end_loc_text.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                //Set dest address in viewmodel
                viewmodel.setDestAddr(end_loc_text.text.toString())
                destAddress = viewmodel.getdestAddr().value!!
                end_loc_text.setText(destAddress)

                lifecycleScope.launch{
                    (activity as MapsActivity).toLatLng(end_loc_text.text.toString(),1)
                    delay(1000L)
                    Log.d("setDestAddrr", destAddress)
                }
                true
            } else {
                false
            }
        }

        calc_button.setOnClickListener {
            startLatitude = viewmodel.getStartLatitude().value
            startLongitude = viewmodel.getStartLong().value
            destLatitude = viewmodel.getDestLatitude().value
            destLongitude = viewmodel.getDestLong().value
            Log.d("","$startLatitude is:  ")
            if(startLatitude != 0.0 && destLongitude != 0.0 &&
                destLatitude != 0.0 && destLongitude != 0.0) {
                (activity as MapsActivity).getDistance(startLatitude!!, startLongitude!!, destLatitude!!, destLongitude!!)
                //When API is called, keep trying to update the value
                lifecycleScope.launch{
                    var updated = false
                    var counter = 0
                    while(updated == false && counter < 12){
                        //Poll every 750ms
                        delay(750L)
                        //If load time is long, slow down polling to once every 10 seconds
                        if(counter > 7){
                            delay(10000L)
                        }
                        distanceT.setText(viewmodel.getDistance().value.toString() + " Miles")
                        costT.setText(viewmodel.getGasPrice().value)
                        counter += 1
                    }
                }
            }
        }
    }

    companion object {
    }
}