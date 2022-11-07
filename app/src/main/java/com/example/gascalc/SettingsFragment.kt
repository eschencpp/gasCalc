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
import com.example.gascalc.databinding.FragmentSettingsBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    //View Binding
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentSettingsBinding.inflate(layoutInflater)
        arguments?.let {
        }
        dataStore = context?.createDataStore(name = "settings")!!

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
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.saveButton.setOnClickListener{
            lifecycleScope.launch{
                save(
                    "mpg",
                    binding.enterMPG.text.toString()
                )
                val Tmpg = read("mpg")
                Toast.makeText(activity,"Your MPG is: $Tmpg",Toast.LENGTH_LONG).show()
            }

        }

        binding.readMPGButton.setOnClickListener {
            lifecycleScope.launch{
                val x  = read("mpg")
                Log.d("TAG", "The value is $x")
                //Toast.makeText(activity.applicationContext,"Your MPG is:",Toast.LENGTH_LONG)
            }

        }
        return view

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