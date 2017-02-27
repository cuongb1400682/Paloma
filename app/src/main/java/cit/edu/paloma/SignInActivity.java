package cit.edu.paloma;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GithubAuthCredential;
import com.google.firebase.auth.GithubAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import io.fabric.sdk.android.Fabric;


public class SignInActivity
        extends AppCompatActivity
        implements View.OnClickListener, FirebaseAuth.AuthStateListener, GoogleApiClient.OnConnectionFailedListener {
    private static final int RC_GOOGLE_SIGN_IN = 0;
    private static final String TAG = SignInActivity.class.getSimpleName();
    private Button mGooglePlusButton;
    private Button mFacebookButton;
    private Button mTwitterButton;
    private Button mGithubButton;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private ConstraintLayout mWaitingLayout;
    private ConstraintLayout mSignInLayout;
    private String mServiceName;
    private CallbackManager mCallbackManager;
    private LoginButton mFacebookLoginButton;
    private TwitterLoginButton mTwitterLoginButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        initViews();


        mAuth = FirebaseAuth.getInstance();

        mCallbackManager = CallbackManager.Factory.create();
        mFacebookLoginButton = new LoginButton(this);
        Log.v(TAG, mFacebookButton.toString());
        mFacebookLoginButton.setReadPermissions("email", "public_profile");

        mFacebookLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                firebaseLogInWithFacebook(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                new AlertDialog.Builder(SignInActivity.this, R.style.DialogTheme)
                        .setIcon(R.mipmap.ic_aborted)
                        .setMessage("Facebook login cancelled")
                        .show();
            }

            @Override
            public void onError(FacebookException error) {
                String message = error.getMessage();
                new AlertDialog.Builder(SignInActivity.this, R.style.DialogTheme)
                        .setIcon(R.mipmap.ic_failed)
                        .setTitle("Log in failed")
                        .setMessage(message)
                        .show();
            }
        });

        TwitterAuthConfig twitterAuthConfig = new TwitterAuthConfig(
                getString(R.string.twitter_consumer_key),
                getString(R.string.twitter_consumer_secret)
        );
        Fabric.with(this, new Twitter(twitterAuthConfig));
        mTwitterLoginButton = new TwitterLoginButton(this);
        mTwitterLoginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(com.twitter.sdk.android.core.Result<TwitterSession> result) {
                firebaseLogInWithTwitter(result.data);
            }

            @Override
            public void failure(TwitterException exception) {
                String message = exception.getMessage();
                new AlertDialog.Builder(SignInActivity.this, R.style.DialogTheme)
                        .setIcon(R.mipmap.ic_failed)
                        .setTitle("Log in failed")
                        .setMessage(message)
                        .show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(this);
    }

    private void initViews() {
        mGooglePlusButton = (Button) findViewById(R.id.google_plus_button);
        mFacebookButton = (Button) findViewById(R.id.facebook_button);
        mTwitterButton = (Button) findViewById(R.id.twitter_button);
        mGithubButton = (Button) findViewById(R.id.github_button);
        mWaitingLayout = (ConstraintLayout) findViewById(R.id.waiting_layout);
        mSignInLayout = (ConstraintLayout) findViewById(R.id.sign_in_layout);

        mGooglePlusButton.setOnClickListener(this);
        mFacebookButton.setOnClickListener(this);
        mTwitterButton.setOnClickListener(this);
        mGithubButton.setOnClickListener(this);
    }

    private void showWaitingScreen(boolean yes) {
        if (yes) {
            mSignInLayout.setVisibility(View.INVISIBLE);
            mWaitingLayout.setVisibility(View.VISIBLE);
        } else {
            mSignInLayout.setVisibility(View.VISIBLE);
            mWaitingLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.google_plus_button:
                signInWithGoogle();
                mServiceName = "google";
                break;
            case R.id.facebook_button:
                mServiceName = "facebook";
                mFacebookLoginButton.callOnClick();
                break;
            case R.id.twitter_button:
                mServiceName = "twitter";
                mTwitterLoginButton.callOnClick();
                break;
            case R.id.github_button:
                mServiceName = "github";
                firebaseLogInWithGithub();
                break;
        }
    }

    private void signInWithGoogle() {
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        Intent intent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(intent, RC_GOOGLE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_GOOGLE_SIGN_IN:
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                firebaseSignInWithGoogle(result);
                break;
        }
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        mTwitterLoginButton.onActivityResult(requestCode, resultCode, data);
    }

    private void firebaseSignInWithGoogle(GoogleSignInResult result) {
        showWaitingScreen(true);
        if (!result.isSuccess()) {
            new AlertDialog.Builder(this, R.style.DialogTheme)
                    .setIcon(R.mipmap.ic_failed)
                    .setTitle("Sign-in failed")
                    .setMessage(result.getStatus().getStatusMessage())
                    .show();
        } else {
            GoogleSignInAccount account = result.getSignInAccount();

            AuthCredential cred = GoogleAuthProvider.getCredential(account.getIdToken(), null);

            mAuth
                    .signInWithCredential(cred)
                    .addOnCompleteListener(this, mOnCompleteListener);
        }
    }

    private void firebaseLogInWithFacebook(AccessToken accessToken) {
        showWaitingScreen(true);

        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());

        mAuth
                .signInWithCredential(credential)
                .addOnCompleteListener(this, mOnCompleteListener);
    }

    private void firebaseLogInWithTwitter(TwitterSession data) {
        showWaitingScreen(true);

        AuthCredential credential = TwitterAuthProvider.getCredential(
                data.getAuthToken().token,
                data.getAuthToken().secret
        );

        mAuth
                .signInWithCredential(credential)
                .addOnCompleteListener(mOnCompleteListener);
    }

    private void firebaseLogInWithGithub() {
        showWaitingScreen(true);

        AuthCredential credential = GithubAuthProvider.getCredential(getString(R.string.github_access_key));

        mAuth
                .signInWithCredential(credential)
                .addOnCompleteListener(mOnCompleteListener);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            Log.v(TAG, mUser.getDisplayName());
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Log.v(TAG, "user is null");
        }
    }

    private OnCompleteListener<AuthResult> mOnCompleteListener = new OnCompleteListener<AuthResult>() {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            showWaitingScreen(false);
            if (!task.isSuccessful()) {
                Exception exception = task.getException();

                String message = "Cannot sign in with your " + mServiceName + " account";
                if (exception != null) {
                    message = exception.getMessage();
                }

                new AlertDialog.Builder(SignInActivity.this, R.style.DialogTheme)
                        .setIcon(R.mipmap.ic_failed)
                        .setTitle("Sign-in failed")
                        .setMessage(message)
                        .show();
            }
        }
    };

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
