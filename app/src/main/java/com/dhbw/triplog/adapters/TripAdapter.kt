package com.dhbw.triplog.adapters

import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dhbw.triplog.R
import com.dhbw.triplog.db.Trip
import com.dhbw.triplog.other.DataUtility
import com.dhbw.triplog.other.TrackingUtility
import kotlinx.android.synthetic.main.item_trip.view.*

class TripAdapter : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView)

    val diffCallback = object : DiffUtil.ItemCallback<Trip>() {
        override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)

    fun submitList (list : List<Trip>) = differ.submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        return TripViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_trip,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = differ.currentList[position]
        holder.itemView.apply {
            Glide.with(this).load(trip.img).into(ivTripImage)

            tvDate.text = DataUtility.getFormattedDate(trip.timestamp)
            tvTime.text = TrackingUtility.getFormattedStopWatchTime(trip.timeInMillis)
            tvLabel.text = trip.label?.let { DataUtility.retrieveLabelFromJSON(it).label }

            if(trip.uploadStatus) {
                tvUpload.text = "Uploaded"
                cvTrip.setBackgroundColor(ContextCompat.getColor(context, R.color.color_Uploaded))
            } else {
                tvUpload.text = "Not Uploaded"
                cvTrip.setBackgroundColor(ContextCompat.getColor(context, R.color.color_notUploaded))
            }
        }
    }

}