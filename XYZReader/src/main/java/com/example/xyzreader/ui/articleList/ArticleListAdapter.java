package com.example.xyzreader.ui.articleList;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.customViews.DynamicHeightNetworkImageView;
import com.example.xyzreader.ui.helpers.ImageLoaderHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by USUARIO on 15/01/2018.
 */

public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ViewHolder> {

    private String TAG = this.getClass().getSimpleName();

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    private Context mContext;
    private Cursor mCursor;

    public ArticleListAdapter(Context context, Cursor cursor) {
        mContext    = context;
        mCursor     = cursor;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context         = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.list_item_article, parent, false);
        return new ViewHolder(view);
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        mCursor.moveToPosition(position);

        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
        Date publishedDate = parsePublishedDate();
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {

        holder.subtitleView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                    publishedDate.getTime(),
                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL).toString()
                    + "<br/>" + " by "
                    + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        } else {
                    holder.subtitleView.setText(Html.fromHtml(
                    outputFormat.format(publishedDate)
                    + "<br/>" + " by "
                    + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        }
        holder.thumbnailView.setImageUrl(
            mCursor.getString(ArticleLoader.Query.THUMB_URL),
            ImageLoaderHelper.getInstance(mContext).getImageLoader()
        );

        holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Transition with shared view.
                Bundle bundle = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    ArticleListActivity activity = (ArticleListActivity) mContext;
                    bundle = ActivityOptions.makeSceneTransitionAnimation(activity,holder.thumbnailView,holder.thumbnailView.getTransitionName()).toBundle();
                    //bundle = ActivityOptions.makeSceneTransitionAnimation(activity).toBundle();
                }


                // Launching Detail Activity.
                Intent activityIntent = new Intent(Intent.ACTION_VIEW, ItemsContract.Items.buildItemUri(getItemId(holder.getAdapterPosition())));
                mContext.startActivity(activityIntent,bundle);
                //mContext.startActivity(activityIntent);
            }
        });

        }

    @Override
    public int getItemCount() {
            return mCursor.getCount();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        DynamicHeightNetworkImageView thumbnailView;
        TextView titleView;
        TextView subtitleView;

        ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}
