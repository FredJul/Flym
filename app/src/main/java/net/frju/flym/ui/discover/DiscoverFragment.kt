package net.frju.flym.ui.discover

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import net.fred.feedex.R
import net.frju.flym.GlideApp
import net.frju.flym.data.entities.Feed
import org.jetbrains.anko.layoutInflater


class DiscoverFragment : Fragment(), AdapterView.OnItemClickListener {

    companion object {
        const val TAG = "DiscoverFragment"

        @JvmStatic
        fun newInstance() = DiscoverFragment()
    }

    private lateinit var manageFeeds: FeedManagementInterface

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)
        initGridView(view)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        manageFeeds = context as FeedManagementInterface
    }

    private fun initGridView(view: View) {
        val gvTopics: GridView = view.findViewById(R.id.gv_topics)
        val topics = view.context.resources.getStringArray(R.array.discover_topics)
        gvTopics.adapter = TopicAdapter(view.context, topics)
        gvTopics.onItemClickListener = this
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val topic = parent?.getItemAtPosition(position) as String
        manageFeeds.searchForFeed("#$topic")
    }

    class TopicAdapter(context: Context, topics: Array<String>) :
            ArrayAdapter<String>(context, R.layout.item_discover_topic, topics) {

        private class ItemViewHolder {
            var image: ImageView? = null
            var title: TextView? = null
        }

        private fun setTopicTitle(viewHolder: ItemViewHolder, topic: String) {
            viewHolder.title?.text = topic
        }

        private fun setTopicImage(viewHolder: ItemViewHolder, topic: String) {
            val letterDrawable = Feed.getLetterDrawable(topic.hashCode().toLong(), topic)
            viewHolder.image?.let { iv ->
                GlideApp.with(context).clear(iv)
                iv.setImageDrawable(letterDrawable)
            }
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            val viewHolder: ItemViewHolder
            var inflatedView: View? = view
            if (inflatedView == null) {
                inflatedView = context.layoutInflater.inflate(R.layout.item_discover_topic, null)
                viewHolder = ItemViewHolder()
                inflatedView?.let { vw ->
                    viewHolder.image = vw.findViewById(R.id.iv_topic_image) as ImageView
                    viewHolder.title = vw.findViewById(R.id.tv_topic_title) as TextView
                }
            } else {
                viewHolder = inflatedView.tag as ItemViewHolder
            }
            val item = getItem(i)
            item?.let { it ->
                setTopicImage(viewHolder, it)
                setTopicTitle(viewHolder, it)
            }
            inflatedView?.tag = viewHolder
            return inflatedView!!
        }
    }
}