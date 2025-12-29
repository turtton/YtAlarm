package net.turtton.ytalarm.ui.selection

import android.util.SparseArray
import android.view.View
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView

class TagKeyProvider<T>(private val recyclerView: RecyclerView) : ItemKeyProvider<T>(SCOPE_CACHED) {
    private val keyArray = SparseArray<T>()

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

        @Suppress("UNCHECKED_CAST")
        val id = holder.itemView.tag as T
        if (pos != RecyclerView.NO_POSITION) {
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

    override fun getKey(position: Int): T? = keyArray[position]

    override fun getPosition(key: T & Any): Int = keyArray.indexOfValue(key)
}