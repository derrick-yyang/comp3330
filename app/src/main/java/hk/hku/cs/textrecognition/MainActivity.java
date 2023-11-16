package hk.hku.cs.textrecognition;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
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
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
    private MaterialButton inputImageBtn;
    private MaterialButton recognizeTextBtn;
    private ShapeableImageView imageIv;
    private EditText recognizedTextEt;

    //TAG
    private static final String TAG = "MAIN_TAG";

    //Uri of the image that we will take from Camera/Gallery
    private Uri imageUri = null;

    //to handle the result of Camera/Gallery permissions
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;

    //arrays of permission required to pick image from Camera, Gallery
    private String[] cameraPermissions;
    private String[] storagePermissions;

    //progress dialog
    private ProgressDialog progressDialog;

    //TextRecognizer
    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init UI Views
        inputImageBtn = findViewById(R.id.inputImageBtn);
        recognizeTextBtn = findViewById(R.id.recognizeTextBtn);
        imageIv = findViewById(R.id.imageIv);
        recognizedTextEt = findViewById(R.id.recognizedTextEt);

        //init arrays of permission required for Camera, Gallery
        cameraPermissions = new String[]{Manifest.permission.CAMERA};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //init setup the progress dialog, show white text from image is being recognized
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //init TextRecognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        //handle click, show input image dialog
        inputImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputImageDialog();
            }
        });
        //handle click, start recognizing text from image we tok from Camera/Gallery
        recognizeTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check if image is picked or not
                if (imageUri == null){
                    Toast.makeText(MainActivity.this, "Pick Image fist...", Toast.LENGTH_SHORT).show();
                }else{
                    recognizeTextFromImage();
                }
            }
        });

    }

    private void recognizeTextFromImage(){
        Log.d(TAG, "recognizeTextFromImage: ");
        //set message and show progress dialog
        progressDialog.setMessage("Preparing image...."); ;
        progressDialog.show();

        try {
            //Prepare InputImage from image uri
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);
            //image prepared, we are about to start text recognition process, change progress message
            progressDialog.setMessage("Recognizing text...");
            //start text recognition process from image
            Task<Text> textTaskResult = textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            //process completed, dismiss dialog
                            progressDialog.dismiss();
                            //get the recognized text
                            String recognizedText = text.getText();
                            Log.d(TAG, "onSuccess: recognizedText: "+recognizedText);
                            //set the recognized text to edit text
                            recognizedTextEt.setText(recognizedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed recognizing text from image, dismiss dialog, show reason in Toast
                            progressDialog.dismiss();
                            Log.d(TAG, "onFailure: ", e);
                            Toast.makeText(MainActivity.this, "Failed recognizing text due to" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e){
            //Exception occurred while preparing InputImage, dismiss dialog, show reason in Toast
            progressDialog.dismiss();
            Log.d(TAG, "recognizeTextFromImage: ", e);
            Toast.makeText(this, "Failed preparing due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void showInputImageDialog() {
        //init PopupMenu param 1 is context, param 2 is UI View Where we want to show PopupMenu
        PopupMenu popupMenu = new PopupMenu( this, inputImageBtn);

        //Add items Camera, Gallery to PopupMenu, param 2 is menu id, param 3 is position of this menu item in menu item list, param 4 is title of the menu
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "GALLERY");

        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == 1) {
                if (checkCameraPermission()) {
                    pickImageCamera();
                } else {
                    requestCameraPermission();
                }
            } else if (id == 2){
                pickImageGallery();
            }
            return true;
        });
    }


    private void pickImageGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri dataUri = result.getData().getData();
                            imageIv.setImageURI(dataUri);
                            imageUri = dataUri;
                        } else {
                            Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    private void pickImageCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            imageIv.setImageURI(imageUri);
                        } else {
                            Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermissions(){

        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return cameraResult && storageResult;
    }

    private void requestCameraPermissions(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    //handle permission results

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}