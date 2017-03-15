package cit.edu.paloma.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.firebase.database.DatabaseException;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

import cit.edu.paloma.R;
import cit.edu.paloma.utils.ImgurUtils;

import static android.provider.MediaStore.*;

public class UserDetailsActivity
        extends AppCompatActivity
        implements View.OnClickListener, LoaderManager.LoaderCallbacks<Intent> {

    private static final int ACTION_REQUEST_CAMERA = 0;
    private static final int ACTION_REQUEST_GALLERY = 1;
    private static final String TAG = UserDetailsActivity.class.getSimpleName();
    private static final int USER_DETAILS_UPDATE_LOADER = 0;

    private Button saveUserProfileButton;
    private EditText fullNameEdit;
    private EditText emailEdit;
    private RadioButton maleRadio;
    private RadioButton femaleRadio;
    private ImageView avatarImage;

    private Vector<Exception> mExceptions = new Vector<>();
    private ProgressDialog mProgressDialog;

    private boolean isAvatarSelected;
    private boolean mCreateNew;
    private String mEmail;
    private String mFullName;
    private String mAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        Bundle bundle = getIntent().getExtras();

        mCreateNew = bundle.getBoolean("createNew");
        mEmail = bundle.getString("email");
        mFullName = bundle.getString("fullName");
        mAvatar = bundle.getString("avatar");

        saveUserProfileButton = (Button) findViewById(R.id.save_user_profile_button);
        fullNameEdit = (EditText) findViewById(R.id.detail_full_name_edit);
        emailEdit = (EditText) findViewById(R.id.detail_email_edit);
        maleRadio = (RadioButton) findViewById(R.id.male_radio);
        femaleRadio = (RadioButton) findViewById(R.id.female_radio);
        avatarImage = (ImageView) findViewById(R.id.avatar_image);

        avatarImage.setOnClickListener(this);
        saveUserProfileButton.setOnClickListener(this);
        maleRadio.setOnClickListener(this);
        femaleRadio.setOnClickListener(this);

        isAvatarSelected = false;

        mProgressDialog = ProgressDialog.show(
                this,
                null,
                "Please wait for saving data...",
                true,
                true
        );

        mProgressDialog.hide();
    }

    private void openAvatarPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image Source");
        builder.setItems(new CharSequence[]{"Gallery", "Camera"}, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");

                        Intent chooser = Intent.createChooser(intent, "Choose a Picture");
                        startActivityForResult(chooser, ACTION_REQUEST_GALLERY);
                        break;
                    case 1:
                        Intent getCameraImage = new Intent(ACTION_IMAGE_CAPTURE);
                        startActivityForResult(getCameraImage, ACTION_REQUEST_CAMERA);
                        break;
                    default:
                        break;
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap bitmap = null;

        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case ACTION_REQUEST_CAMERA:
                if (data != null) {
                    bitmap = (Bitmap) data.getExtras().get("data");
                }
                break;
            case ACTION_REQUEST_GALLERY:
                Uri uri = data.getData();
                try {
                    bitmap = Images.Media.getBitmap(this.getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }

        if (bitmap != null) {
            avatarImage.setImageBitmap(bitmap);
            isAvatarSelected = true;
        }
    }

    private boolean isAllDetailsValid() {
        boolean isValid = true;

        clearErrors();

        if (fullNameEdit.getText().toString().trim().isEmpty()) {
            fullNameEdit.setError("Full name is mandatory");
            isValid = false;
        }

        if (emailEdit.getText().toString().trim().isEmpty()) {
            emailEdit.setError("Email is mandatory");
            isValid = false;
        }

        if (!maleRadio.isChecked() && !femaleRadio.isChecked()) {
            ((TextView) findViewById(R.id.detail_gender_text)).setError("Select the gender");
            isValid = false;
        }

        return isValid;
    }

    @Override
    public synchronized void onClick(View view) {
        switch (view.getId()) {
            case R.id.save_user_profile_button:
                if (isAllDetailsValid()) {
                    getSupportLoaderManager().restartLoader(USER_DETAILS_UPDATE_LOADER, null, this).forceLoad();
                }
                break;
            case R.id.avatar_image:
                openAvatarPicker();
                break;
        }
    }

    private void clearErrors() {
        emailEdit.setError(null);
        fullNameEdit.setError(null);
        ((TextView) findViewById(R.id.detail_gender_text)).setError(null);
    }

    @Override
    public Loader<Intent> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<Intent>(this) {

            @Override
            protected void onStartLoading() {
                super.onStartLoading();
                clearErrors();
                forceLoad();
                mExceptions.clear();
                mProgressDialog.show();
            }

            private String uploadImageToImgur() throws DatabaseException {
                String imageLink = null;

                try {
                    Bitmap bitmap = ((BitmapDrawable) avatarImage.getDrawable()).getBitmap();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                    byte[] base64Image = baos.toByteArray();

                    Response response = null;

                    response = ImgurUtils.uploadBase64Photo(base64Image);

                    if (response == null || !response.isSuccessful()) {
                        mExceptions.add(new DatabaseException("Cannot load image to Imgur."));
                    } else {
                        JSONObject data = new JSONObject(response.body().string()).getJSONObject("data");
                        imageLink = data.getString("link");
                        Log.v(TAG, imageLink);
                    }
                } catch (Exception e) {
                    mExceptions.add(e);
                }

                return imageLink;
            }

            @Override
            public Intent loadInBackground() {
                final String avatarLink = uploadImageToImgur();

                Intent intent = new Intent();

                intent.putExtra("email", emailEdit.getText().toString());
                intent.putExtra("fullName", fullNameEdit.getText().toString());
                intent.putExtra("avatar", avatarLink);
                intent.putExtra("isMale", maleRadio.isChecked());

                return intent;
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Intent> loader, Intent data) {
        mProgressDialog.hide();
        if (!mExceptions.isEmpty()) {
            new AlertDialog.Builder(UserDetailsActivity.this)
                    .setIcon(R.mipmap.ic_failed)
                    .setMessage(mExceptions.elementAt(0).getMessage())
                    .setTitle("Failed")
                    .show();

            for (Exception e : mExceptions) {
                Log.v(TAG, e.getMessage());
            }
        } else {
            new AlertDialog.Builder(UserDetailsActivity.this)
                    .setIcon(R.mipmap.ic_completed)
                    .setMessage("User details successfully saved")
                    .show();

            setResult(RESULT_OK, data);

            if (mCreateNew) {
                finish();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Intent> loader) {

    }
}
