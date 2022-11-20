package com.example.gascalc

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.gascalc.databinding.FragmentBottomBinding
import com.example.gascalc.databinding.FragmentSettingsBinding

class BottomFragment : Fragment() {
    //Viewbinding
    private var _binding: FragmentBottomBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentBottomBinding.inflate(layoutInflater)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBottomBinding.inflate(inflater, container, false)
        val view = binding.root
        // Inflate the layout for this fragment
        return view
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            BottomFragment().apply {
                arguments = Bundle().apply {

                }
            }

    }
}