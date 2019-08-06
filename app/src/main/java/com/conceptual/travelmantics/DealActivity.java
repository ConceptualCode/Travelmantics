package com.conceptual.travelmantics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private static final int PICTURE_RESULT = 42;
    EditText txtTitle;
    EditText txtDescription;
    EditText txtPrice;
    TravelDeal deal;
    ImageView imageView;
    private ProgressBar mProgressBar;
    private Button mBtnImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
      //  FirebaseUtil.openFbReference("traveldeals");
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;
        txtTitle = (EditText) findViewById(R.id.txtTitle);
        txtDescription = (EditText) findViewById(R.id.txtDescription);
        txtPrice = (EditText) findViewById(R.id.txtPrice);
        imageView = (ImageView) findViewById(R.id.image);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
        if (deal == null) {
            deal = new TravelDeal();
        }
        this.deal = deal;
        txtTitle.setText(deal.getTitle());
        txtDescription.setText(deal.getDescription());
        txtPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());
        mBtnImage = findViewById(R.id.btnImage);
        mBtnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
              intent.setType("image/*");
              intent.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
              startActivityForResult(intent.createChooser(intent,"Insert Picture"), PICTURE_RESULT);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
       switch (item.getItemId()) {
           case R.id.save_menu:
                 saveDeal();
               Toast.makeText(this, "Deal Saved", Toast.LENGTH_LONG).show();
               clean();
               backToList();
               return true;
           case R.id.delete_menu:
               deleteDeal();
               Toast.makeText(this, "Deal Deleted", Toast.LENGTH_LONG).show();
               backToList();
               return true;
               default:
                   return super.onOptionsItemSelected(item);
           }
       }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);
        if (FirebaseUtil.isAdmin) {
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditTexts(true);
        }
        else {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditTexts(false);
            mBtnImage.setVisibility(View.GONE);
        }



        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            final StorageReference ref = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            mProgressBar.setVisibility(mProgressBar.VISIBLE);
            mBtnImage.setEnabled(false);
            UploadTask uploadTask = ref.putFile(imageUri);
            Task<Uri> task = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {

                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        mProgressBar.setVisibility(View.GONE);
                        mBtnImage.setEnabled(true);
                        Uri downloadUrl = task.getResult();
                        String url = downloadUrl.toString();
                        String pictureName = task.getResult().getLastPathSegment();
                        deal.setImageUrl(url);
                        deal.setImageName(pictureName);
                        showImage(url);
                    }
                }
            });

        }
    }

    private void saveDeal() {
        deal.setTitle(txtTitle.getText().toString());
        deal.setDescription(txtDescription.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        if (deal.getId() == null) {
            mDatabaseReference.push().setValue(deal);
        } else {
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }
    }

    private void deleteDeal() {
        if (TextUtils.isEmpty(txtTitle.getText()) && TextUtils.isEmpty(txtDescription.getText()) && TextUtils.isEmpty(txtPrice.getText())) {
            Toast.makeText(this, "Please save the deal before deleting", Toast.LENGTH_LONG).show();
            return;
        }
        mDatabaseReference.child(deal.getId()).removeValue();

        if (this.deal.getImageUrl() != null && !this.deal.getImageUrl().isEmpty()) {
            FirebaseUtil.connectStorage();
            StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(this.deal.getImageUrl());
            storageReference.delete();
        }

    }

    private void backToList() {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }
    private void clean() {
        txtTitle.setText("" );
        txtPrice.setText("" );
        txtDescription.setText("");
        txtTitle.requestFocus();

    }
    private void enableEditTexts(boolean isEnabled) {
        txtTitle.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);

    }

    private void showImage(String url) {
       if (url != null && url.isEmpty() == false) {
           int width = Resources.getSystem().getDisplayMetrics().widthPixels;
           Picasso.get()
                   .load(url)
                   .resize(width,width*2/3)
                   .centerCrop()
                   .into(imageView);
       }
    }
}
