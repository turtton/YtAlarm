package net.turtton.ytalarm.util

import android.util.SparseArray
import android.view.View
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView

class StringKeyProvider(
    private val recyclerView: RecyclerView
) : ItemKeyProvider<String>(SCOPE_CACHED) {
    private val keyArray = SparseArray<String>()

    init {
        recyclerView.addOnChildAttachStateChangeListener(
            object : RecyclerView.OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(view: View) = onAttached(view)
                override fun onChildViewDetachedFromWindow(view: View) = onDetached(view)
            }
        )
    }

    fun onAttached(view: View) {
        val holder = recyclerView.findContainingViewHolder(view) ?: return
        val pos = holder.absoluteAdapterPosition
        val id = holder.itemView.tag
        if (pos != RecyclerView.NO_POSITION && id is String) {
            keyArray[pos] = id
        }
    }

    fun onDetached(view: View) {
        val holder = recyclerView.findContainingViewHolder(view) ?: return
        val pos = holder.absoluteAdapterPosition
        val id = holder.itemView.tag
        if (pos != RecyclerView.NO_POSITION && id is String) {
            keyArray.remove(pos)
        }
    }

    override fun getKey(position: Int): String? = keyArray[position]

    override fun getPosition(key: String): Int = keyArray.indexOfValue(key)
}