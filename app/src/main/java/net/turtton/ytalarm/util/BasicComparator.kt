package net.turtton.ytalarm.util

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil

class BasicComparator<T>: DiffUtil.ItemCallback<T>() where T : Any {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem === newItem
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem == newItem
    }
}