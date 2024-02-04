package net.turtton.ytalarm.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import net.turtton.ytalarm.BuildConfig
import net.turtton.ytalarm.R
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.databinding.FragmentAboutBinding
import net.turtton.ytalarm.ui.adapter.AboutPageAdapter

class FragmentAboutPage : Fragment() {
    @Suppress("ktlint:standard:property-naming")
    private var _binding: FragmentAboutBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mainActivity = requireActivity() as MainActivity
        val fab = mainActivity.binding.fab
        fab.visibility = View.GONE

        val version = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE
        val versionText = view.context.getString(R.string.version, version, versionCode)
        binding.fragmentAboutVersion.text = versionText

        val aboutList = binding.fragmentAboutList
        aboutList.layoutManager = LinearLayoutManager(view.context)
        aboutList.adapter = AboutPageAdapter(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}