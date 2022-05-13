package com.example.locationnotes

import android.os.Bundle
import android.widget.ArrayAdapter
import com.example.locationnotes.databinding.ActivityListBinding

class ListActivity : LTActivity() {
    lateinit var binding: ActivityListBinding
    var isActivityResumedForOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()

        if (MainActivity.placeToSelectOnResume != null) {
            finish()
        }

        val placeList = Place.getAll().reversed()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            placeList.map { it.displayDescription }
        )
        binding.placeList.adapter = adapter

        binding.placeList.setOnItemClickListener { _,_,position,_ ->
            showPlaceInfo(placeList[position])
        }

        if (isActivityResumedForOnce && placeList.isEmpty()) {
            finish()
        }
        isActivityResumedForOnce = true
    }
}