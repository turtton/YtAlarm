package net.turtton.ytalarm.ui.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import net.turtton.ytalarm.R
import net.turtton.ytalarm.ui.adapter.AboutPageAdapter.Thumbnail.Url.Companion.toThumbnail
import net.turtton.ytalarm.ui.fragment.FragmentAboutPage

class AboutPageAdapter(
    private val fragment: FragmentAboutPage
) : RecyclerView.Adapter<AboutPageAdapter.ViewHolder>() {

    private val dataSet: Array<AboutPageData>

    init {
        @Suppress("ktlint:standard:max-line-length")
        val github = AboutPageData.LinkEmbedData(
            "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png".toThumbnail(),
            R.string.item_aboutpage_title_github,
            R.string.item_aboutpage_details_github,
            "https://github.com/turtton/YtAlarm"
        )
        val license = AboutPageData.LinkEmbedData(
            "https://www.gnu.org/graphics/gplv3-with-text-136x68.png".toThumbnail(),
            R.string.item_aboutpage_title_license,
            R.string.item_aboutpage_details_license,
            "https://github.com/turtton/YtAlarm/blob/HEAD/LICENSE"
        )
        val thirdPartyLicensesTitle = R.string.item_aboutpage_title_tirdpartylicenses
        val thirdPartyLicense = AboutPageData.LinkEmbedData(
            Thumbnail.Drawable(R.drawable.ic_menu_book),
            thirdPartyLicensesTitle,
            null,
            "https://app.fossa.com/projects/custom%2B34065%2Fgithub.com%2Fturtton%2FYtAlarm"
        )

        @Suppress("ktlint:standard:max-line-length")
        val liberapay = AboutPageData.LinkEmbedData(
            "https://en.liberapay.com/assets/liberapay/icon-v2_white-on-yellow.200.png?save_as=liberapay_logo_white-on-yellow_200px.png".toThumbnail(),
            R.string.item_aboutpage_liberapay_title,
            R.string.item_aboutpage_liberapay_details,
            "https://liberapay.com/turtton/donate"
        )

        @Suppress("ktlint:standard:max-line-length")
        val bitcoin = AboutPageData.CopyableData(
            "https://camo.githubusercontent.com/d8f6d0e0aeafda23077ad6fdccd927fff87e1fff516534465269ebd9cffdaf4b/68747470733a2f2f656e2e626974636f696e2e69742f772f696d616765732f656e2f322f32392f42435f4c6f676f5f2e706e67".toThumbnail(),
            R.string.item_aboutpage_bitcoin_title,
            R.string.item_aboutpage_bitcoin_address,
            "3C3aj9pXf6xSm5im4ZMtmS3HeoGpBNtD7t"
        )
        dataSet = arrayOf(github, license, thirdPartyLicense, liberapay, bitcoin)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.item_aboutpage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val view = holder.itemView
        val context = view.context
        val data = dataSet[position]
        holder.title.text = context.getString(data.title)
        data.details?.let {
            holder.details.text = context.getString(it)
        } ?: kotlin.run {
            holder.details.visibility = View.GONE
        }

        val icon = holder.icon
        when (val thumbnail = data.thumbnail) {
            is Thumbnail.Url -> {
                Glide.with(view).load(thumbnail.url).into(icon)
            }
            is Thumbnail.Drawable -> icon.setImageResource(thumbnail.id)
        }

        val onClickAction: (View) -> Unit = when (data) {
            is AboutPageData.LinkEmbedData -> { _ ->
                val intent = Intent(Intent.ACTION_VIEW, data.url.toUri())
                fragment.startActivity(intent)
            }
            is AboutPageData.CopyableData -> action@{ v ->
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        ?: kotlin.run {
                            val message = R.string.snackbar_error_failed_to_access_clipboard
                            Snackbar.make(v, message, Snackbar.LENGTH_SHORT).show()
                            return@action
                        }
                val clipData = ClipData.newPlainText("YtAlarmClipData", data.clipData)
                clipboard.setPrimaryClip(clipData)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    val message = R.string.snackbar_copied
                    Snackbar.make(v, message, Snackbar.LENGTH_SHORT).show()
                }
            }
            is AboutPageData.ActionData -> data.action
        }

        view.setOnClickListener(onClickAction)
    }

    override fun getItemCount(): Int = dataSet.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_aboutpage_title)
        val details: TextView = view.findViewById(R.id.item_aboutpage_details)
        val icon: ImageView = view.findViewById(R.id.item_aboutpage_icon)
    }

    private sealed interface AboutPageData {
        val thumbnail: Thumbnail
        val title: Int
        val details: Int?

        data class LinkEmbedData(
            override val thumbnail: Thumbnail,
            @StringRes
            override val title: Int,
            @StringRes
            override val details: Int?,
            val url: String
        ) : AboutPageData

        data class CopyableData(
            override val thumbnail: Thumbnail,
            @StringRes
            override val title: Int,
            @StringRes
            override val details: Int?,
            val clipData: String
        ) : AboutPageData

        data class ActionData(
            override val thumbnail: Thumbnail,
            @StringRes
            override val title: Int,
            @StringRes
            override val details: Int?,
            val action: (View) -> Unit
        ) : AboutPageData
    }

    private sealed interface Thumbnail {
        @JvmInline
        value class Url(val url: String) : Thumbnail {
            companion object {
                fun String.toThumbnail() = Url(this)
            }
        }

        @JvmInline
        value class Drawable(@DrawableRes val id: Int) : Thumbnail
    }
}