package com.dhbw.triplog.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhbw.triplog.R
import com.dhbw.triplog.adapters.TripAdapter
import com.dhbw.triplog.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_info.*

@AndroidEntryPoint
class InfoFragment : Fragment(R.layout.fragment_info) {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var tripAdapter: TripAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        viewModel.trips.observe(viewLifecycleOwner, Observer {
            tripAdapter.submitList(it)
        })

    }

    private fun setupRecyclerView() = rvTrips.apply {
        tripAdapter = TripAdapter()
        adapter = tripAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }
}