package com.example.textrecognition;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.widget.EditText;
import android.Manifest;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class MainActivity extends AppCompatActivity {
    //UI Views
    private MaterialButton btn1, btn2;
    private ShapeableImageView imageView;
    private EditText recognizedText;
    //TAG
    private static final String TAG = "MAIN_TAG";
    //to handle request of camera and gallery image picking
    private static final int STORAGE_REQUEST_CODE = 101, CAMERA_REQUEST_CODE = 100;
    private Uri imageUri = null;
    //list of permission required to pick/take image from gallery/camera
    private String[] cameraPermissions, storagePermissions;

    //progress dialog
    private ProgressDialog progressDialog;

    //Text Recognizer
    private TextRecognizer textRecognizer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init UI Views
        btn1 = findViewById(R.id.b1);
        btn2 = findViewById(R.id.b2);
        imageView = findViewById(R.id.imgV);
        recognizedText = findViewById(R.id.recText);

        //init permission arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //init progress dialog
        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please Wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //init Text Recognizer
        textRecognizer= TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        //handle click, show image dialog
        btn1.setOnClickListener(e -> {
            showInputImageDialog();
        });

        //handle click, start recognizing text from image we took from Camera/Gallery
        btn2.setOnClickListener(e->{
            //check if image is picked or not
            if(imageUri==null)
                //if null, that means image is not been picked yet...
                Toast.makeText(MainActivity.this, "Pick image first...", Toast.LENGTH_SHORT).show();
            else
                //if not null, that means image is picked, so we can start recognizing the text from it
                recognizedTextFromImage();
        });
    }

    private void recognizedTextFromImage() {
        Log.d(TAG, "recognizedTextFromImage: ");
        progressDialog.setMessage("Preparing Image... Kindly Hold Tight...");
        progressDialog.show();
        //surrounded with try and catch just in case something goes wrong and we end up in an exception
        try {
            InputImage inputImage=InputImage.fromFilePath(this,imageUri);
            progressDialog.setMessage("Recognizing Text... Nearly There...");
            progressDialog.show();
            //start text recognition process from image
            Task<Text> textTaskResult=textRecognizer.process(inputImage)
                    .addOnSuccessListener(e->{
                        //dismiss dialog on process completion
                        progressDialog.dismiss();
                        //get and set the text to editable text field
                        recognizedText.setText(e.getText());
                        Log.d(TAG, "onSuccess: recognizedTextFromImage: "+recognizedText);
                    })
                    .addOnFailureListener(e->{
                        //failed recognizing text from image, dismissed dialog, reason shown in toast
                        progressDialog.dismiss();
                        Log.e(TAG, "onFailure: ", e);
                        Toast.makeText(MainActivity.this,"Failed recognizing text due to "+e.getMessage(),Toast.LENGTH_SHORT).show();
                    });

        }catch (Exception e){
            //exception occurred while preparing InputImage, dismissed dialog, reason shown in toast
            progressDialog.dismiss();
            Log.e(TAG, "recognizedTextFromImage: ", e);
            Toast.makeText(MainActivity.this,"Failed recognizing text due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showInputImageDialog() {
        /*init PopupMenu
        * param 1 is context
        * param 2 is menu
        * param 3 is position of this menu item in menu items list,
        * param 4 is title of the menu
        * */
        PopupMenu popupMenu=new PopupMenu(this, btn1);
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "GALLERY");
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(e->{
            //get item id that is clicked from PopupMenu
            int id=e.getItemId();
            if(id==1){
                //Camera is clicked, Check permissions are granted or not
                Log.d(TAG, "onMenuItemClick: Camera Clicked...");
                if(checkCameraPermission())
                    //if permissions are granted, we can launch camera intent
                    pickImageCamera();
                else
                    //if permissions are not granted, request camera permissions
                    requestCameraPermission();
            }
            else if(id==2){
                //Gallery is clicked, Check permissions are granted or not
                Log.d(TAG, "onMenuItemClick: Gallery Clicked...");
                if(checkStoragePermission())
                    //if permissions are granted, user can choose from gallery
                    pickImageGallery();
                else
                    //if permissions are not granted, request storage permissions
                    requestStoragePermission();
            }
            return true;
        });
    }

    private void pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //if picked, image is received here
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //image picked
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri "+imageUri);
                        //set to imageview
                        imageView.setImageURI(imageUri);
                    } else {
                        Log.d(TAG, "onActivityResult: cancelled");
                        //cancelled
                        Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void pickImageCamera() {
        Log.d(TAG, "pickImageCamera: ");
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.TITLE, "Sample Title");
        values.put(MediaStore.Video.Media.DESCRIPTION, "Sample Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //if image taken from camera, it'll be received here
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // image is taken from camera
                        //we already have the image in imageUri using function pickImageCamera()
                        Log.d(TAG, "onActivityResult: imageUri " + imageUri);
                        imageView.setImageURI(imageUri);
                    }
                    else { // cancelled
                        Log.d(TAG, "onActivityResult: cancelled");
                        Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private boolean checkStoragePermission() {
        //check if storage permission is allowed, and return true/false regarding same
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission() {
        //request storage permission (for picking image from gallery)
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        //check if camera and storage permission is allowed, and return true/false regarding same
        return (
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED)
                        &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED)
        );
    }

    private void requestCameraPermission() {
        //request camera permission (for camera intent)
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    //handle permission results

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                //Check if some action from permission dialog performed or not Allow/Deny
                if(grantResults.length>0){
                    //Check if Camera, Storage permissions granted, contains boolean results either true or false
                    boolean cameraAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted=grantResults[1]==PackageManager.PERMISSION_GRANTED;
                    //Check if both permissions are granted or not
                    if(cameraAccepted && storageAccepted)
                        //both permissions (Camera and Gallery) are granted, we can launch camera intent
                        pickImageCamera();
                    else
                        //one or both permissions are denied, can't launch camera intent
                        Toast.makeText(this, "Camera and Storage Permissions are required", Toast.LENGTH_SHORT).show();
                }
                else
                    //Neither allowed not denied, rather cancelled
                    Toast.makeText(this, "Cancelled...", Toast.LENGTH_SHORT).show();
            }

            break;

            case STORAGE_REQUEST_CODE: {
                //Check if some action from permission dialog performed or not Allow/Deny
                if(grantResults.length>0){
                    //Check if Storage permissions granted, contains boolean results either true or false
                    boolean storageAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    //Check if storage permission is granted or not
                    if(storageAccepted)
                        //storage permission granted, we can launch gallery intent
                        pickImageGallery();
                    else //storage permission denied, can't launch gallery intent
                        Toast.makeText(this,"Storage Permission is required...", Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }
    }
}