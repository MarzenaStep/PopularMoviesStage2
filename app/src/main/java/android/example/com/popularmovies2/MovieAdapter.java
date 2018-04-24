package android.example.com.popularmovies2;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.List;


//The main idea how to create adapter comes from https://guides.codepath.com/android/using-the-recyclerview

//Creating basic adapter which extends from RecycleView.Adapter
public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.PosterAdapterViewHolder>  {
    private final Context mContext;
    private List<Movie> mPosterList;
    //Defining ClickHandler member variable
    final private PosterAdapterOnClickHandler mClickHandler;

    //Defining ClickHandler interface to handle click on items within PosterAdapter
    public interface PosterAdapterOnClickHandler {
        void onClick(Movie movie);
    }

    public MovieAdapter(Context context, List<Movie> posterList, PosterAdapterOnClickHandler clickHandler) {
        mContext = context;
        mPosterList = posterList;
        mClickHandler = clickHandler;
    }

    public void clearMovieList() {
        mPosterList.clear();
        notifyDataSetChanged();

    }
    public void updateMoveList(List<Movie> movies) {
        mPosterList = movies;
        notifyDataSetChanged();
    }

    // Inflating a layout from XML and returning new holder
    @Override
    public PosterAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.poster_list_item, parent, false);
        rootView.setFocusable(true);
        return new PosterAdapterViewHolder(rootView);
    }

    // Populating data into the item through holder
    @Override
    public void onBindViewHolder(PosterAdapterViewHolder holder, int position) {

        //Get data model based on position
        Movie currentMovie = mPosterList.get(position);

        String posterUrl;
        if (currentMovie != null) {
            posterUrl = currentMovie.getPosterUrl();
            Picasso.with(mContext).load(posterUrl).placeholder(R.drawable.ic_launcher_background).into(holder.posterIv);
        }
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        if (mPosterList == null) {
            return 0;
        } else {
            return mPosterList.size();
        }
    }

    class PosterAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final ImageView posterIv;

        public PosterAdapterViewHolder(View itemView) {
            super(itemView);
            posterIv = itemView.findViewById(R.id.iv_poster);
            itemView.setOnClickListener(this);
        }

        // Handles the movie item being clicked
        @Override
        public void onClick(View view) {
            int adapterPosition = getAdapterPosition();
            Movie currentMovie = mPosterList.get(adapterPosition);
            mClickHandler.onClick(currentMovie);
        }
    }
}
