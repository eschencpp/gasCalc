package com.example.gascalc

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.createDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.example.gascalc.databinding.FragmentSettingsBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    //View Binding
    private lateinit var viewmodel : sharedViewModel
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private  var gMPG : String? = null
    private  var gasPrice : String? = null
    private lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentSettingsBinding.inflate(layoutInflater)
        arguments?.let {
        }
        dataStore = context?.createDataStore(name = "settings")!!

        lifecycleScope.launch(){
            if(read("mpg") != null){
                gMPG = read("mpg")
                binding.enterMPG.setText(read("mpg") + "  MPG")
            }
            if(read("gas") != null){
                gasPrice = read("gas")
                binding.gasPriceText.setText(read("gas" ) + "  $/gallon")
            }
        }
    }

    private suspend fun save(key : String, value:String){
        val dataStoreKey = preferencesKey<String>(key)
        dataStore.edit { settings ->
            settings[dataStoreKey] = value
        }
    }

    private suspend fun read(key : String): String?{
        val dataStoreKey = preferencesKey<String>(key)
        val preferences = dataStore.data.first()
        return preferences[dataStoreKey]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewmodel = ViewModelProvider(requireActivity()).get(sharedViewModel::class.java)
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.enterMPG.setOnFocusChangeListener{ _, hasFocus ->
            if (hasFocus)
                enterMPG.setText("")
            else
                enterMPG.setText(gMPG + "  MPG")
        }

        binding.gasPriceText.setOnFocusChangeListener{ _, hasFocus ->
            if (hasFocus)
                binding.gasPriceText.setText("")
            else
                binding.gasPriceText.setText(gasPrice + "  $/gallon")
        }

        binding.saveMPGButton.setOnClickListener{
            if(binding.enterMPG.text.toString() != ""
                && isNumeric(binding.enterMPG.text.toString())) {
                lifecycleScope.launch {
                    save(
                        "mpg",
                        binding.enterMPG.text.toString()
                    )
                    gMPG = read("mpg")
                    binding.enterMPG.setText(gMPG + "  MPG")
                    viewmodel.setMPG(read("mpg")!!)
                    Toast.makeText(activity, "Your MPG is: $gMPG", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.saveGasButton.setOnClickListener {
            if(binding.gasPriceText.text.toString() != ""
                && isNumeric(binding.gasPriceText.text.toString()) ) {
                lifecycleScope.launch {
                    save(
                        "gas",
                        binding.gasPriceText.text.toString()
                    )
                    gasPrice = read("gas")
                    binding.gasPriceText.setText(gasPrice + "  $/gallon")
                    viewmodel.setGasPrice(read("gas")!!)
                }
            }
        }
        return view
    }

    private fun isNumeric(str : String): Boolean{
        return str.toDoubleOrNull() != null
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment SettingsFragment.
         */
        @JvmStatic
        fun newInstance() =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}
