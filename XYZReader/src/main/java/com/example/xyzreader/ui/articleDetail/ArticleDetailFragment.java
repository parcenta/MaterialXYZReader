package com.example.xyzreader.ui.articleDetail;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.helpers.ImageLoaderHelper;
import com.example.xyzreader.ui.articleList.ArticleListActivity;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final String SHOWING_FULL_BODY_TEXT_BOOLEAN = "SHOWING_FULL_BODY_TEXT_BOOLEAN";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;

    private ImageView mPhotoView;
    private Button mShowMoreBodyText;
    private Toolbar mToolbar;
    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private LinearLayout mMetaBar;



    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    private static String fullBodyText;
    private static boolean showFullBodyText;
    private String articleTitle;
    private boolean showToolbarTitle = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set this flag initially as false.
        showFullBodyText = false;

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(savedInstanceState!=null && savedInstanceState.containsKey(SHOWING_FULL_BODY_TEXT_BOOLEAN))
            showFullBodyText = savedInstanceState.getBoolean(SHOWING_FULL_BODY_TEXT_BOOLEAN,false);

        // Setting the toolbar in the Parent Activity.
        ArticleDetailActivity parentActivity = (ArticleDetailActivity) getActivity();
        parentActivity.setToolbarInActivity(mToolbar);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Hey. I recommend reading this book...")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        mShowMoreBodyText = (Button) mRootView.findViewById(R.id.show_more_detail);
        mShowMoreBodyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFullBodyText = true;
                mShowMoreBodyText.setVisibility(View.GONE);
                showBodyText();
            }
        });

        mToolbar                    = (Toolbar) mRootView.findViewById(R.id.toolbar);
        mCollapsingToolbarLayout    = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsing_toolbar_layout);
        mMetaBar                    = (LinearLayout) mRootView.findViewById(R.id.meta_bar);
        mAppBarLayout               = (AppBarLayout) mRootView.findViewById(R.id.app_bar_layout);

        // Initially set the title as empty...
        mToolbar.setTitle(" ");
        mCollapsingToolbarLayout.setTitle(" ");

        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {



                if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    // Collapsed
                    showToolbarTitle = true;
                } else{
                    // Expanded or semi-expanded.
                    showToolbarTitle = false;
                }

                updateToolbarTitle();
            }
        });

        return mRootView;
    }

    private void updateToolbarTitle(){
        if(mToolbar == null) return;

        if(showToolbarTitle && articleTitle != null && !articleTitle.isEmpty())
            mToolbar.setTitle(articleTitle);
        else
            mToolbar.setTitle(" ");
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

    private void bindViews() {
        if (mRootView == null) {
            return;
        }



        TextView titleView  = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);

            articleTitle = mCursor.getString(ArticleLoader.Query.TITLE);

            titleView.setText(articleTitle);
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }

            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.generate(bitmap, 12);
                                mMutedColor = p.getDarkMutedColor(0xFF333333);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                mMetaBar.setBackgroundColor(mMutedColor);
                                mCollapsingToolbarLayout.setContentScrimColor(mMutedColor);
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                        }
                    });

            // Manage the full bodytext to be shown. We dont want to show the whole mody text at first.
            showBodyText();

            updateToolbarTitle();

        } else {
            mRootView.setVisibility(View.GONE);
            mCollapsingToolbarLayout.setTitle("N/A");
            bylineView.setText("N/A" );
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }else
            bindViews();


        // -------------------------------------------------------------
        // Start the postponed Enter Transition of the parent activity.
        // -------------------------------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ArticleDetailActivity parentActivity = getActivityCast();
            if(parentActivity!= null) parentActivity.startPostponedEnterTransition();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    private void showBodyText(){
        if (mCursor==null) return;

        fullBodyText = mCursor.getString(ArticleLoader.Query.BODY);

        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (fullBodyText!=null && !fullBodyText.isEmpty()) {
            String bodyTextToBeShown = showFullBodyText ? fullBodyText : fullBodyText.substring(0, 2000) + "...";
            bodyTextToBeShown = bodyTextToBeShown.replaceAll("\r\n\r\n", "<br/><br/>");
            bodyTextToBeShown = bodyTextToBeShown.replaceAll("\r\n", " ");
            bodyTextToBeShown = bodyTextToBeShown.replaceAll("\n", "<br/>");
            bodyView.setText(Html.fromHtml(bodyTextToBeShown));
        }else
            bodyView.setText("N/A");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SHOWING_FULL_BODY_TEXT_BOOLEAN,showFullBodyText);
    }
}
