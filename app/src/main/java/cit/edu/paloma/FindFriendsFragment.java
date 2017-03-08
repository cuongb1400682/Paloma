package cit.edu.paloma;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.TextViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import cit.edu.paloma.adapters.SuggestedFriendListAdapter;
import cit.edu.paloma.datamodals.User;


public class FindFriendsFragment extends Fragment {
    private View rootView;
    private ListView mSuggestedFriendsList;
    private SuggestedFriendListAdapter mAdapter;
    private TextView mNoResultText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_find_friends, container, false);
        initViews();
        return rootView;
    }

    private void initViews() {
        mAdapter = new SuggestedFriendListAdapter(getContext());

        mSuggestedFriendsList = (ListView) rootView.findViewById(R.id.suggested_friends_list);
        mSuggestedFriendsList.setAdapter(mAdapter);

        mNoResultText = (TextView) rootView.findViewById(R.id.no_result_text);
    }

    public void setListOfUsers(ArrayList<User> listOfUsers) {
        mAdapter = new SuggestedFriendListAdapter(getContext());
        mAdapter.addAll(listOfUsers);
        mSuggestedFriendsList.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        if (listOfUsers.isEmpty()) {
            mNoResultText.setVisibility(View.VISIBLE);
        } else {
            mNoResultText.setVisibility(View.GONE);
        }
    }

    public void showProgressBar(boolean b) {
        // todo: show progress bar
    }
}
