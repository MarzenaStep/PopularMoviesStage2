package android.example.com.popularmovies2.activities;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.example.com.popularmovies2.BuildConfig;
import android.example.com.popularmovies2.objects.Movie;
import android.example.com.popularmovies2.adapters.MovieAdapter;
import android.example.com.popularmovies2.loaders.MovieLoader;
import android.example.com.popularmovies2.R;
import android.example.com.popularmovies2.databinding.ActivityMainBinding;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MainActivity  extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Movie>>, SharedPreferences.OnSharedPreferenceChangeListener, MovieAdapter.PosterAdapterOnClickHandler {

        //Tag for logging. It's best to use the class's name using getSimpleName as it helps to identify the location from which logs are being posted.
        public static final String LOG_TAG = MainActivity.class.getSimpleName();


        private static final String API_KEY = BuildConfig.MY_MOVIE_DB_API_KEY;

        //URL for movie data sorted by popularity key
        public static final String POPULAR_URL = "https://api.themoviedb.org/3/movie/popular?api_key=" + API_KEY;

        //URL for movie data sorted by top rated key
        public static final String TOP_RATED_URL = "https://api.themoviedb.org/3/movie/top_rated?api_key=" + API_KEY;

        // This constant String will be used to store movies list
        private static final String MOVIES_KEY = "movies";

        // Constant value for the movie loader ID. It can be any integer.
        private static final int MOVIE_LOADER_ID = 1;

        private MovieAdapter movieAdapter;
        private GridLayoutManager layoutManager;
        private ActivityMainBinding mainBinding;
        private ArrayList<Movie> moviesList;


        private static boolean PREFERENCES_UPDATED = false;


        @Override
        protected void onCreate(Bundle savedInstanceState) {

            Log.i(LOG_TAG, "TEST: MainActivity onCreate() called ");

            super.onCreate(savedInstanceState);
            mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

            /*
         * If savedInstanceState is not null, that means our Activity is not being started for the
         * first time. Even if the savedInstanceState is not null, it is smart to check if the
         * bundle contains the key we are looking for. In our case, the key we are looking for maps
         * to the contents of the posterAdapter that displays list of movies. If the bundle
         * contains that key, we set the contents of the poster list accordingly.
         */
            //If the savedInstanceState bundle is not null, update trailer list
            if (savedInstanceState != null) {
                if (savedInstanceState.containsKey(MOVIES_KEY)) {
                    moviesList = savedInstanceState.getParcelableArrayList(MOVIES_KEY);
                }
            }


            //https://discussions.udacity.com/t/autofit-grid-recycler-view/188314
            Resources resources = getResources();
            int numOfColumns = resources.getInteger(R.integer.list_columns_number);

            layoutManager = new GridLayoutManager(this, numOfColumns);

            // Attach layout manager to the RecyclerView
            mainBinding.rvPoster.setLayoutManager(layoutManager);
            mainBinding.rvPoster.setHasFixedSize(true);


            movieAdapter = new MovieAdapter(this, new ArrayList<Movie>(), this);

            mainBinding.rvPoster.setAdapter(movieAdapter);

            movieAdapter.notifyDataSetChanged();


            // Get a reference to the ConnectivityManager to check state of network connectivity
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            assert connMgr != null;

            // Get details on the currently active default data network
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

            // If there is a network connection, fetch data
            if (networkInfo != null && networkInfo.isConnected()) {

                // Get a reference to the LoaderManager, in order to interact with loaders
                LoaderManager loaderManager = getLoaderManager();

                Log.i(LOG_TAG, "TEST: calling initLoader() ...");

                // Initialize the loader
                loaderManager.initLoader(MOVIE_LOADER_ID, null, this);

            } else {
                // Hide loading indicator and display error message - no internet connection
                mainBinding.pbLoadingIndicator.setVisibility(View.GONE);
                mainBinding.tvErrorMessage.setText(R.string.no_internet_connection);
            }

            PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public Loader<List<Movie>> onCreateLoader(int loaderId, Bundle bundle) {
            // Create a new loader for the given URL
            Log.i(LOG_TAG, "TEST: onCreateLoader() called ...");

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            String keyForSortOrder = this.getString(R.string.settings_sort_order_key);
            String defaultSortOrder = this.getString(R.string.settings_sort_order_by_default);
            String preferredSortOrder = sharedPref.getString(keyForSortOrder, defaultSortOrder);

            boolean userPrefersPopular = false;
            if (defaultSortOrder.equals(preferredSortOrder) == true) {
                layoutManager.scrollToPosition(0);
                setTitle(getString(R.string.settings_most_popular_label));
                return new MovieLoader(this, POPULAR_URL);
            }

            if (userPrefersPopular == false) {
                layoutManager.scrollToPosition(0);
                setTitle(getString(R.string.settings_top_rated_label));
                return new MovieLoader(this, TOP_RATED_URL);

            }
            return null;



        }



        @Override
        public void onLoadFinished(Loader<List<Movie>> loader, List<Movie> movies) {

            Log.i(LOG_TAG, "TEST: onLoadFinished() called ...");

            // Hide loading indicator and errorMessageTv because the data has been loaded
            mainBinding.pbLoadingIndicator.setVisibility(View.GONE);
            mainBinding.tvErrorMessage.setVisibility(View.INVISIBLE);

            //Clear out existing data
            movieAdapter.clearMovieList();



            //If there is a valid list of {@link Movie}s, then add them to the adapter's data set otherwise display error message - no movies found
            if (movies != null && !movies.isEmpty()) {
                movieAdapter.updateMoveList(movies);
            }
            else {
                mainBinding.tvErrorMessage.setVisibility(View.VISIBLE);
                mainBinding.tvErrorMessage.setText(R.string.no_movies);
            }


        }

        @Override
        public void onLoaderReset(Loader<List<Movie>> loader) {

            Log.i(LOG_TAG, "TEST: onLoaderReset() called ...");
            // Clear out existing data
            movieAdapter.clearMovieList();
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main, menu);
            //Return true so that the menu is displayed in the Toolbar
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {

            int id = item.getItemId();

            //Launch SettingsActivity when the Settings option is clicked
            if (id == R.id.action_settings) {
                Intent startSettingsActivity = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(startSettingsActivity);
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        protected void onStart() {
            super.onStart();

            if (PREFERENCES_UPDATED) {
                getLoaderManager().restartLoader(MOVIE_LOADER_ID, null, this);
                PREFERENCES_UPDATED = false;
            }
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            PREFERENCES_UPDATED = true;

        }
    
    //This method is for responding to clicks from movie list
        @Override
        public void onClick(Movie movie) {
            //Launch DetailsActivity when movie poster is clicked
            Intent startDetailActivityIntent = new Intent(MainActivity.this, DetailsActivity.class);
            startDetailActivityIntent.putExtra(DetailsActivity.SELECTED_MOVIE_INFORMATION, movie);
            startActivity(startDetailActivityIntent);
        }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
            savedInstanceState.putParcelableArrayList(MOVIES_KEY, moviesList);
        super.onSaveInstanceState(savedInstanceState);
    }
}

