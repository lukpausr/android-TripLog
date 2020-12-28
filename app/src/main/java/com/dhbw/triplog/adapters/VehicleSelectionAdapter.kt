package com.dhbw.triplog.adapters

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dhbw.triplog.R

/**
 * RecyclerView Adapter for the vehicle selection
 * Influenced and adapted from:
 * https://medium.com/android-beginners/popupwindow-android-example-in-kotlin-5919245c8b8a
 *
 * @property vehicleList
 * @property selectedItem
 * @property callback
 */
class VehicleSelectionAdapter(val context: Context) : RecyclerView.Adapter<VehicleSelectionAdapter.VehicleViewHolder>() {

    var vehicleList : List<VehicleItem> = mutableListOf()

    private var selectedItem: Int = -1
    var callback: RecyclerViewCallback<VehicleItem>? = null

    /**
     * Adds vehicles to the selector menu and uses this list as content for the recycler view
     *
     * @param allVehicles List containing vehicles of type VehicleItem
     */
    fun addVehicle(allVehicles: List<VehicleItem>) {
        vehicleList = allVehicles.toMutableList()
        notifyDataSetChanged()
    }

    /**
     * Change the selected Item and notify the recycler View about the change on selection
     *
     * @param position New position / selected item
     */
    fun selectedItem(position: Int){
        selectedItem = position
        notifyItemChanged(position)
    }

    /**
     * Being called by RecyclerView to associate a ViewHolder with data.
     * See: https://developer.android.com/guide/topics/ui/layout/recyclerview#implement-adapter
     * for detailed information
     *
     * @param holder
     * @param position
     */
    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val item = vehicleList[position]
        holder.tvVehicleName.text = item.name
        holder.ivVehicleIcon.background = ContextCompat.getDrawable(context, item.icon)

        if(position == selectedItem) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.ivVehicleIcon.backgroundTintList = ContextCompat.getColorStateList(context, R.color.dhbw_red)
            }
            holder.tvVehicleName.setTextColor(ContextCompat.getColor(context, R.color.dhbw_red))
            holder.ivVehicleSelected.visibility = View.VISIBLE
        } else {
            holder.ivVehicleSelected.visibility = View.INVISIBLE
        }
    }

    /**
     * Set onClick listener
     *
     * @param click
     */
    fun setOnClick(click: RecyclerViewCallback<VehicleItem>){
        callback = click
    }

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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        return VehicleViewHolder(
            LayoutInflater.from(parent.context).inflate(
                    R.layout.item_vehicle,
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
        return vehicleList.size
    }

    /**
     * Inner class VehicleViewHolder being used for the VehicleItem references, extending from
     * RecyclerView.ViewHolder, allowing VehicleSelectionAdapter to use it as ViewHolder
     *
     * @param itemView
     *
     * @property tvVehicleName TextView containing Vehicle Name
     * @property ivVehicleIcon ImageView containing Vehicle Vector Graphic
     * @property ivVehicleSelected ImageView for indicating which vehicle is currently selected
     * @property layoutVehicleItem Layout of the View
     */
    inner class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var tvVehicleName: TextView = this.itemView.findViewById(R.id.tvVehicleName)
        var ivVehicleIcon: ImageView = this.itemView.findViewById(R.id.ivVehicleIcon)
        var ivVehicleSelected: ImageView = this.itemView.findViewById(R.id.ivVehicleSelected)
        var layoutVehicleItem: ConstraintLayout = this.itemView.findViewById(R.id.layoutVehicleItem)

        /**
         * Initializer being used to call the setClickListener methods for the item to react on
         * click input by the user
         */
        init {
            setClickListener(layoutVehicleItem)
        }

        /**
         * Applying a click listener to the current View
         *
         * @param view View which the Click listener is going to be applied to
         */
        private fun setClickListener(view: View) {
            view.setOnClickListener {
                callback?.onItemClicked(it, adapterPosition, vehicleList[adapterPosition])
            }
        }
    }
}

/**
 * Interface for implementing a onItemClick listener
 */
interface RecyclerViewCallback<T> {
    fun onItemClicked(view: View, position: Int, item: T)
}

/**
 * Data class being used to define the different items shown in the popup window in the trip
 * fragment
 */
data class VehicleItem(val icon: Int, val name: String)