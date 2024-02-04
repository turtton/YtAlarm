package net.turtton.ytalarm.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import net.turtton.ytalarm.databinding.FragmentListBinding

abstract class FragmentAbstractList : Fragment() {
    @Suppress("ktlint:standard:property-naming")
    private var _binding: FragmentListBinding? = null

    protected val binding get() = _binding!!

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}