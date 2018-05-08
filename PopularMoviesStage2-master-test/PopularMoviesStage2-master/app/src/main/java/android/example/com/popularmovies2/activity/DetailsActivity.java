package android.example.com.popularmovies2.activity;


import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.databinding.DataBindingUtil;
import android.example.com.popularmovies2.BuildConfig;
import android.example.com.popularmovies2.R;
import android.example.com.popularmovies2.adapter.ReviewAdapter;
import android.example.com.popularmovies2.adapter.TrailerAdapter;
import android.example.com.popularmovies2.data.MovieContract.MovieEntry;
import android.example.com.popularmovies2.data.MovieDbHelper;
import android.example.com.popularmovies2.databinding.ActivityDetailsBinding;
import android.example.com.popularmovies2.loader.ReviewLoader;
import android.example.com.popularmovies2.loader.TrailerLoader;
import android.example.com.popularmovies2.object.Movie;
import android.example.com.popularmovies2.object.Review;
import android.example.com.popularmovies2.object.Trailer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


//The idea how to display selected movie information comes from Sandwich Club Project
public class DetailsActivity extends AppCompatActivity implements TrailerAdapter.TrailerAdapterOnClickHandler {

    //Tag for logging. It's best to use the class's name using getSimpleName as it helps to identify the location from which logs are being posted.
    private static final String LOG_TAG = DetailsActivity.class.getSimpleName();

    public static final String SELECTED_MOVIE_INFORMATION = "selected_movie_information";

    //Url for MovieDB trailers http://api.themoviedb.org/3/movie/157336/videos?api_key=###
    //Url for MovieDB reviews http://api.themoviedb.org/3/movie/83542/reviews?api_key=###
    public static final String BASE_MOVIE_DB = "https://api.themoviedb.org/3/movie/";

    // This constant String will be used to store trailers list
    private static final String TRAILERS_KEY = "trailers";

    //This constant String will be used tp store reviews list
    private static final String REVIEWS_KEY = "reviews";

    private Movie selectedMovie;
    //Declare an ActivityDetailsBinding field called mDetailsBinding
    private ActivityDetailsBinding detailsBinding;
    // Constant value for the trailer loader ID
    private static final int TRAILER_LOADER_ID = 2;
    // Constant value for the review loader ID
    private static final int REVIEW_LOADER_ID = 3;

    //Database helper that will provide us access to the database
    private MovieDbHelper movieDbHelper;

    private TrailerAdapter trailerAdapter;
    private ReviewAdapter reviewAdapter;
    private LinearLayoutManager trailersLayoutManager;
    private LinearLayoutManager reviewsLayoutManager;
    private String movieId;
    private static final String API_KEY = BuildConfig.MY_MOVIE_DB_API_KEY;
    private String trailerUrl;
    private String reviewUrl;
    private ArrayList<Trailer> trailersList;
    private ArrayList<Review> reviewsList;


    //https://stackoverflow.com/questions/15414206/use-different-asynctask-loaders-in-one-activity
    private class ReviewCallback implements LoaderManager.LoaderCallbacks<List<Review>> {

        @Override
        public Loader<List<Review>> onCreateLoader(int i, Bundle bundle) {
            // Create a new loader for the given URL
            Log.i(LOG_TAG, "TEST: onCreateLoader() called ...");
            movieId = String.valueOf(selectedMovie.getId());
            Log.e(LOG_TAG, "This is the movie id");
            reviewUrl = BASE_MOVIE_DB + movieId + "/reviews?api_key=" + API_KEY;
            Log.e(LOG_TAG, "This is review final URL");
            return new ReviewLoader(getApplicationContext(), reviewUrl);
        }

        @Override
        public void onLoadFinished(Loader<List<Review>> loader, List<Review> reviews) {
            //If there is a valid list of {@link Review}s, then add them to the adapter's data set otherwise display error message - no movies found
            if (reviews != null && !reviews.isEmpty()) {
                reviewAdapter.updateReviewList(reviews);
                int numberOfReviews = reviews.size();
                String amountOfReviews = getResources().getQuantityString(R.plurals.reviews, numberOfReviews, numberOfReviews);
                detailsBinding.tvReviewNumber.setText(amountOfReviews);
            } else {
                detailsBinding.tvReviewNumber.setText(getString(R.string.details_no_reviews));
            }
        }

        @Override
        public void onLoaderReset(Loader<List<Review>> loader) {
        }
    }

    private class TrailerCallback implements LoaderManager.LoaderCallbacks<List<Trailer>> {
        @Override
        public Loader<List<Trailer>> onCreateLoader(int loaderId, Bundle bundle) {
            // Create a new loader for the given URL
            Log.i(LOG_TAG, "TEST: onCreateLoader() called ...");
            movieId = String.valueOf(selectedMovie.getId());
            Log.e(LOG_TAG, "This is the movie id");
            trailerUrl = BASE_MOVIE_DB + movieId + "/videos?api_key=" + API_KEY;
            Log.e(LOG_TAG, "This is movie final URL");
            return new TrailerLoader(getApplicationContext(), trailerUrl);
        }


        @Override
        public void onLoadFinished(Loader<List<Trailer>> loader, List<Trailer> trailers) {

            //If there is a valid list of {@link Movie}s, then add them to the adapter's data set otherwise display error message - no movies found
            if (trailers != null && !trailers.isEmpty()) {
                trailerAdapter.updateTrailerList(trailers);
                int numberOfTrailers = trailers.size();
                String amountOfTrailers = getResources().getQuantityString(R.plurals.videos, numberOfTrailers, numberOfTrailers);
                detailsBinding.tvVideosNumber.setText(amountOfTrailers);
            }
            else detailsBinding.tvVideosNumber.setText(getString(R.string.details_no_trailers));
        }

        @Override
        public void onLoaderReset(Loader<List<Trailer>> loader) {
            Log.i(LOG_TAG, "TEST: onLoaderReset() called ...");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Instantiate mDetailBinding using DataBindingUtil
        detailsBinding = DataBindingUtil.setContentView(this, R.layout.activity_details);

        // To access our database, we instantiate our subclass of SQLiteOpenHelper
        // and pass the context, which is the current activity.
       movieDbHelper= new MovieDbHelper(this);

        /*
         * If savedInstanceState is not null, that means Activity is not being started for the
         * first time. Even if the savedInstanceState is not null, it is smart to check if the
         * bundle contains the key. In our case, the key we are looking for maps
         * to the contents of the trailerAdapter that displays our list of trailers. If the bundle
         * contains that key, we set the contents of the trailer list accordingly.
         */
        //If the savedInstanceState bundle is not null, update trailer list
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(TRAILERS_KEY)) {
                trailersList = savedInstanceState.getParcelableArrayList(TRAILERS_KEY);
            } else if (savedInstanceState.containsKey(REVIEWS_KEY)) {
                reviewsList = savedInstanceState.getParcelableArrayList(REVIEWS_KEY);
            }
        }

        Intent movieIntent = getIntent();
        if (movieIntent != null && movieIntent.hasExtra(SELECTED_MOVIE_INFORMATION)) {
            selectedMovie = movieIntent.getParcelableExtra(SELECTED_MOVIE_INFORMATION);
        } else {
            closeOnError();
        }

        trailersLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        reviewsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        detailsBinding.rvTrailers.setLayoutManager(trailersLayoutManager);
        detailsBinding.rvReview.setLayoutManager(reviewsLayoutManager);
        detailsBinding.rvTrailers.setHasFixedSize(true);
        detailsBinding.rvReview.setHasFixedSize(true);
        detailsBinding.rvTrailers.setNestedScrollingEnabled(false);
        detailsBinding.rvReview.setNestedScrollingEnabled(false);
        detailsBinding.tbFavourites.setChecked(false);
        detailsBinding.tbFavourites.setBackgroundDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_border_black_24dp));
        detailsBinding.tbFavourites.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                if (isChecked) {
                    detailsBinding.tbFavourites.setBackgroundDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_black_24dp));
                    insertMovieToFavourites();
                }
                else {
                    detailsBinding.tbFavourites.setBackgroundDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_border_black_24dp));
                }
            }
        });


        trailerAdapter = new TrailerAdapter(this, new ArrayList<Trailer>(), this);
        detailsBinding.rvTrailers.setAdapter(trailerAdapter);
        trailerAdapter.notifyDataSetChanged();

        reviewAdapter = new ReviewAdapter(this, new ArrayList<Review>());
        detailsBinding.rvReview.setAdapter(reviewAdapter);
        reviewAdapter.notifyDataSetChanged();

        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        assert connMgr != null;

        // Get details on the currently active default data network
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        // If there is a network connection, fetch data
        if (networkInfo != null && networkInfo.isConnected()) {

            // Get a reference to the LoaderManager, in order to interact with loaders
            android.app.LoaderManager loaderManager = getLoaderManager();

            Log.i(LOG_TAG, "TEST: calling initLoader() ...");

            // Initialize the loader
            loaderManager.initLoader(TRAILER_LOADER_ID, null, new TrailerCallback());
            loaderManager.initLoader(REVIEW_LOADER_ID, null, new ReviewCallback());

        } else {
            Toast.makeText(this, "No internet", Toast.LENGTH_SHORT).show();
        }


        populateUI();
        Picasso.with(this).load(selectedMovie.getPosterUrl()).placeholder(R.drawable.ic_launcher_background).into(detailsBinding.ivPoster);
        setTitle(selectedMovie.getTitle());
        Picasso.with(this).load(selectedMovie.getBackdropUrl()).into(detailsBinding.ivBackground);


    }

    private void insertMovieToFavourites() {
        // Create a ContentValues object where column names are the keys and selected movie attributes are the values.
        ContentValues movieValues = new ContentValues();
        movieValues.put(MovieEntry.COLUMN_MOVIE_ID, selectedMovie.getId());
        movieValues.put(MovieEntry.COLUMN_MOVIE_ORIGINAL_TITLE, selectedMovie.getOriginalTitle());
        movieValues.put(MovieEntry.COLUMN_MOVIE_TITLE, selectedMovie.getTitle());
        movieValues.put(MovieEntry.COLUMN_MOVIE_OVERVIEW, selectedMovie.getOverview());
        movieValues.put(MovieEntry.COLUMN_MOVIE_BACKDROP_URL, selectedMovie.getBackdropUrl());
        movieValues.put(MovieEntry.COLUMN_MOVIE_POSTER_URL, selectedMovie.getPosterUrl());
        movieValues.put(MovieEntry.COLUMN_MOVIE_VOTE_AVERAGE, selectedMovie.getVoteAverage());
        movieValues.put(MovieEntry.COLUMN_MOVIE_RELEASE_DATE, selectedMovie.getReleaseDate());


        // Insert a new row for selected movie into the provider using the ContentResolver.
        // Use the {@link MovieEntry#CONTENT_URI} to indicate that we want to insert
        // into the movies database table.
        // Receive the new content URI that will allow us to access selected movie data in the future.
       Uri newUri = getContentResolver().insert(MovieEntry.CONTENT_URI, movieValues);
    }

    private void deleteMovieFromFavourites() {

    }


    private void populateUI() {
        detailsBinding.tvOriginalTitle.setText(selectedMovie.getOriginalTitle());
        detailsBinding.tvOverview.setText(selectedMovie.getOverview());
        detailsBinding.tvReleaseDate.setText(selectedMovie.getReleaseDate());

        String averageRating = String.valueOf(selectedMovie.getVoteAverage()) + getString(R.string.details_vote_average_max);
        detailsBinding.tvVotesAverage.setText(averageRating);

    }

    private void closeOnError() {
        finish();
        Toast.makeText(this, R.string.details_error_message, Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelableArrayList(TRAILERS_KEY, trailersList);
        savedInstanceState.putParcelableArrayList(REVIEWS_KEY, reviewsList);
        super.onSaveInstanceState(savedInstanceState);
    }

    //https://stackoverflow.com/questions/574195/android-youtube-app-play-video-intent
    @Override
    public void onClick(Trailer trailer) {
        String video = trailer.getYouTubeFinalUrl();
        String trailerKey = trailer.getTrailerKey();
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + trailerKey));
        Log.e(LOG_TAG, "appIntent: ");
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(video));
        Log.e(LOG_TAG, "webIntent: ");
        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            startActivity(webIntent);
        }
    }

    }





