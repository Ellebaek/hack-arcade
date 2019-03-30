package com.ferring.arcade;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.CreateCollectionRequest;
import com.amazonaws.services.rekognition.model.CreateCollectionResult;
import com.amazonaws.services.rekognition.model.DeleteCollectionRequest;
import com.amazonaws.services.rekognition.model.DeleteCollectionResult;
import com.amazonaws.services.rekognition.model.FaceRecord;
import com.amazonaws.services.rekognition.model.IndexFacesRequest;
import com.amazonaws.services.rekognition.model.IndexFacesResult;
import com.amazonaws.services.rekognition.model.ListCollectionsRequest;
import com.amazonaws.services.rekognition.model.ListCollectionsResult;
import com.amazonaws.services.rekognition.model.ListFacesRequest;
import com.amazonaws.services.rekognition.model.ListFacesResult;
import com.amazonaws.services.rekognition.model.Image;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    AmazonRekognitionClient amazonRekognitionClient;
    String currentPhotoPath;
    Uri currentPhotoUri;
    Bitmap currentBitmap = null;
    int image_index = R.drawable.download;
    Image image = new Image();
    String CollectionID = "your_collection_id";
    String ExternalID = "your_Image_ExternalID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getCredential();
        //attach an instance of HandleClick to the Button
        findViewById(R.id.button_capture).setOnClickListener(new HandleClick());
    }

    private class HandleClick implements View.OnClickListener {
        public void onClick(View arg0) {
            Log.w("", "THEL: I was clicked");
            //Button btn = (Button)arg0; //cast view to a button
            dispatchTakePictureIntent();
            // update the TextView text
            //((TextView)findViewById(R.id.textView)).setText("You pressed " + btn.getText());
        }
    }

    static final int REQUEST_TAKE_PHOTO = 1;
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.w("", "THEL: Request code: " + requestCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK)
        {
            //Bundle extras = data.getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");

            Uri imageUri = currentPhotoUri; //data.getData();
            currentBitmap = null;
            try{
                currentBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                ImageView imageView = findViewById(R.id.imageView0);
                if (currentBitmap != null) {
                    int rotateImage = getCameraPhotoOrientation(currentPhotoPath);

                    imageView.setRotation(rotateImage);
                    imageView.setImageBitmap(currentBitmap);
                    new AsyncTaskRunner().execute();
//                    String response = IndexFaces();
//                    Log.w("0","Result: " + response);
                }
            } catch (Exception e) {
                // file not found
                Log.e("", "Error getting hold of bitmap: ", e);
            }
        }
    }

    private void dispatchTakePictureIntent() {
        if (isEmulator()) {
            currentBitmap = BitmapFactory.decodeResource(getResources(), image_index);
            new AsyncTaskRunner().execute();
        }
        else {

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                Log.w("", "THEL: camera is in place");
                // Ceate the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    Log.w("", "THEL: photo file created");
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.w("", "THEL: photo file not created");
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "com.example.android.fileprovider",
                            photoFile);
                    currentPhotoUri = photoURI;
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    takePictureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    public static int getCameraPhotoOrientation(String imagePath) {
        int rotate = 0;
        try {
            ExifInterface exif  = null;
            try {
                exif = new ExifInterface(imagePath);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            switch (orientation) {

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                default:
                    rotate = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }


    public void getCredential(){
        amazonRekognitionClient = new AmazonRekognitionClient(new AWSCredentialProvider());
        amazonRekognitionClient.setRegion(Region.getRegion(Regions.AP_NORTHEAST_1));    //Put Your AWS Region here
    }

    public String ListCollections(){
        ListCollectionsRequest request = new ListCollectionsRequest();
        ListCollectionsResult response = amazonRekognitionClient.listCollections(request);
        return response.toString();
    }

    public String CreateCollection(){
        CreateCollectionRequest request = new CreateCollectionRequest().withCollectionId(CollectionID);
        CreateCollectionResult response = amazonRekognitionClient.createCollection(request);
        String statusCode = response.getStatusCode().toString();
        return "statusCode: "+statusCode+": "+response.toString();
    }

    public String DeleteCollection(){
        DeleteCollectionRequest request = new DeleteCollectionRequest().withCollectionId(CollectionID);
        DeleteCollectionResult response = amazonRekognitionClient.deleteCollection(request);
        String statusCode = response.getStatusCode().toString();
        return "statusCode: "+statusCode+": "+response.toString();
    }

    public String IndexFaces(){
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), image_index);
        //Bitmap bitmap = null;
        //try {
        //    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), currentPhotoUri);
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}
        Bitmap bitmap = currentBitmap;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
        ByteBuffer imageBytes = ByteBuffer.wrap(stream.toByteArray());
        image.withBytes(imageBytes);
        //DeleteCollection();
        CreateCollection();

        IndexFacesRequest request = new IndexFacesRequest()
                .withCollectionId(CollectionID)
                .withImage(image)
                .withExternalImageId(ExternalID)
                .withDetectionAttributes("ALL");
        IndexFacesResult response = amazonRekognitionClient.indexFaces(request);
        List<FaceRecord> faceRecords = response.getFaceRecords();
        StringBuilder sb = new StringBuilder();
        for (FaceRecord faceRecord : faceRecords) {
            sb.append("Face ID: " + faceRecord.getFace().getFaceId().toString() + "\n");
            sb.append("Emotions: " + faceRecord.getFaceDetail().getEmotions().toString() + "\n");
        }
        DeleteCollection();
        return sb.toString();
    }

    private class AsyncTaskRunner extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            String response = "";
            try {
                response = IndexFaces();
            } catch (AmazonServiceException e) {
                response = e.getErrorMessage();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.w("0","Result: "+result);
            ((TextView)findViewById(R.id.textViewResult)).setText(result);
        }
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}