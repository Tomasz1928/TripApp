package com.example.tripapp2.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripapp2.R
import com.example.tripapp2.ui.dashboard.TripUiModel

class TripAdapter(
    private val onTripClick: (TripUiModel) -> Unit,
    private val onJoinClick: () -> Unit,
    private val onCreateClick: () -> Unit
) : ListAdapter<TripAdapterItem, RecyclerView.ViewHolder>(TripDiffCallback()) {

    companion object {
        private const val TYPE_TRIP = 0
        private const val TYPE_PLACEHOLDER = 1
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return when (val item = getItem(position)) {
            is TripAdapterItem.Trip -> item.trip.hashCode().toLong()
            is TripAdapterItem.Placeholder -> -1L
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TripAdapterItem.Trip -> TYPE_TRIP
            is TripAdapterItem.Placeholder -> TYPE_PLACEHOLDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_TRIP -> {
                val view = inflater.inflate(R.layout.item_trip_card, parent, false)
                TripViewHolder(view, onTripClick)
            }
            TYPE_PLACEHOLDER -> {
                val view = inflater.inflate(R.layout.item_placeholder_card, parent, false)
                PlaceholderViewHolder(view, onJoinClick, onCreateClick)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TripAdapterItem.Trip -> (holder as TripViewHolder).bind(item.trip)
            is TripAdapterItem.Placeholder -> (holder as PlaceholderViewHolder).bind()
        }
    }

    /**
     * Submituje listę wycieczek (ZMIENIONA NAZWA - było submitList)
     */
    fun submitTrips(trips: List<TripUiModel>) {
        val items = trips.map { TripAdapterItem.Trip(it) }
        submitList(items)
    }

    /**
     * Submituje stan pusty (placeholder)
     */
    fun submitEmptyState() {
        submitList(listOf(TripAdapterItem.Placeholder))
    }
}

sealed class TripAdapterItem {
    data class Trip(val trip: TripUiModel) : TripAdapterItem()
    object Placeholder : TripAdapterItem()
}

class TripDiffCallback : DiffUtil.ItemCallback<TripAdapterItem>() {
    override fun areItemsTheSame(oldItem: TripAdapterItem, newItem: TripAdapterItem): Boolean {
        return when {
            oldItem is TripAdapterItem.Trip && newItem is TripAdapterItem.Trip ->
                oldItem.trip.id == newItem.trip.id
            oldItem is TripAdapterItem.Placeholder && newItem is TripAdapterItem.Placeholder ->
                true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: TripAdapterItem, newItem: TripAdapterItem): Boolean {
        return oldItem == newItem
    }
}