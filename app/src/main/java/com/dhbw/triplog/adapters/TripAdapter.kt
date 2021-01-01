package com.dhbw.triplog.adapters

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

/**
 * Adapter class extending RecyclerView.Adapter being used to show all trips in the UploadFragment
 *
 * @property differ
 */
class TripAdapter : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    /**
     * Inner class TripViewHolder being used for the Trip references, extending from
     * RecyclerView.ViewHolder, allowing TripAdapter to use it as ViewHolder
     */
    inner class TripViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView)

    /**
     * Anonymous inner class overrides DiffUtil.ItemCallback which is used to determine
     * changes in the given lists as described in AsnycListDiffer.
     * See: https://developer.android.com/reference/androidx/recyclerview/widget/DiffUtil.ItemCallback
     * for detailed information
     *
     * @see differ
     */
    private val diffCallback = object : DiffUtil.ItemCallback<Trip>() {

        /**
         * Determines if given items are the same by comparing their IDs
         *
         * @param oldItem Item of the old list
         * @param newItem Item of the new list
         *
         * @return Returning whether items are the same or not, described as boolean
         */
        override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Determines if given items content is the same by comparing their hashCodes
         *
         * @param oldItem Item of the old list
         * @param newItem Item of the new list
         *
         * @return Returning whether contents of items are the same or not, described as boolean
         */
        override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    /**
     * AsyncListDiffer of DiffUtil class (Helper). It will dispatch position updates to the
     * TripAdapter when detecting changes between submitted lists
     * See: https://developer.android.com/reference/androidx/recyclerview/widget/AsyncListDiffer
     * for detailed information
     */
    private val differ = AsyncListDiffer(this, diffCallback)

    /**
     * Being called in the Info Fragment (called on mutable live data events) to send an updated
     * (new) list to the AdapterHelper AsyncListDiffer. AsyncListDiffer will detect differences
     * between both lists and react by updating the TripAdapter
     * See: https://developer.android.com/reference/androidx/recyclerview/widget/AsyncListDiffer#submitList(java.util.List%3CT%3E)
     * for detailed information
     *
     * @param list List with elements of type 'Trip'
     */
    fun submitList (list : List<Trip>) = differ.submitList(list)

    /**
     * Being automatically called whenever a new ViewHolder needs to be created. Creates and
     * Initializes the ViewHolder and the associated view but doesn't fill the view's contents.
     * See: https://developer.android.com/guide/topics/ui/layout/recyclerview#implement-adapter
     * for detailed information
     *
     * @param parent
     * @param viewType
     *
     * @return A ViewHolder with a view but without any content
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        return TripViewHolder(
            LayoutInflater.from(parent.context).inflate(
                // Card View Layout file
                R.layout.item_trip,
                parent,
                false
            )
        )
    }

    /**
     * Overridden getItemCount method which is returning the current number of elements
     *
     * @return Current number of elements in list
     */
    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    /**
     * Being called by RecyclerView to associate a ViewHolder with data.
     * See: https://developer.android.com/guide/topics/ui/layout/recyclerview#implement-adapter
     * for detailed information
     *
     * @param holder
     * @param position
     */
    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = differ.currentList[position]
        holder.itemView.apply {

            // Load the Google Maps Screenshot (Bitmap) into the ImageView
            Glide.with(this).load(trip.img).into(ivTripImage)

            // Fill the text view elements with data
            tvDate.text = DataUtility.getFormattedDate(trip.timestamp)
            tvTime.text = TrackingUtility.getFormattedStopWatchTime(trip.timeInMillis)
            tvLabel.text = trip.label?.let { DataUtility.retrieveLabelFromJSON(it).label }

            // Set the uploadStatus text view with data depending on the items uploadStatus which
            // is a boolean (conversion boolean to text) and change the color of items cardView
            // element to match the upload status (green = uploaded, red = not uploaded)
            if(trip.uploadStatus) {
                tvUpload.text = resources.getString(R.string.UPLOAD_SUCCESSFUL)
                cvTrip.setBackgroundColor(ContextCompat.getColor(context, R.color.color_Uploaded))
            } else {
                tvUpload.text = resources.getString(R.string.UPLOAD_NOT_SUCCESSFUL)
                cvTrip.setBackgroundColor(ContextCompat.getColor(context, R.color.color_notUploaded))
            }
        }
    }
}