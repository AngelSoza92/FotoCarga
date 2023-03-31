package com.example.fotocargas;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity  {

    private ImageButton btnBuscar, btnCamara;
    private Button btnSubir;
    byte[] byteArray;
    private ImageView imageView;
    ConexionFOTOCARGA laConexion = new ConexionFOTOCARGA();
    private EditText editTextName;
    String carga;
    private Bitmap imgBitmap;
    ProgressDialog pd;

    private int PICK_IMAGE_REQUEST = 1;
    public static final int REQUEST_CODE_TAKE_PHOTO = 0 /*1*/;

    private String mCurrentPhotoPath;
    private Uri photoURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        byteArray = null;
        btnBuscar = (ImageButton) findViewById(R.id.btBuscar);
        btnSubir = (Button) findViewById(R.id.btSubir);
        btnCamara = (ImageButton) findViewById(R.id.btCamara);
        editTextName = (EditText) findViewById(R.id.editText);

        imageView  = (ImageView) findViewById(R.id.imageView);

        btnBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileChooser();
            }
        });

        btnSubir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iniciarEspera();
                String carga = editTextName.getText().toString().toUpperCase().trim();
                if(carga.equals("")){
                    if (MainActivity.this.pd != null) {
                        MainActivity.this.pd.dismiss();
                    }
                    mensajeCorto("Favor ingresar la CARGA");
                }else{
                    if(byteArray==null){
                        if (MainActivity.this.pd != null) {
                            MainActivity.this.pd.dismiss();
                        }
                        mensajeCorto("Favor seleccionar o tomar la IMAGEN");
                    }else{
                        uploadImage(carga);
                    }
                }
            }
        });

        btnCamara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirCamara();
            }
        });
            checkCameraPermission();
            checkArchivosPermiso();
    }

    public String getStringImagen(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    public void uploadImage(String carga){
        new MainActivity.GuardarFotoCarga().execute(carga);
    }

    public void iniciarEspera(){
        pd = ProgressDialog.show(this, "Enviando", "Espere unos segundos...", true, false);
    }

        class GuardarFotoCarga extends AsyncTask<String, Void, String> {
            boolean exito = false;
            String msgError ="";
            @Override
            protected String doInBackground(String... strings) {
                try {
                    carga = strings[0];
                    Class.forName("com.mysql.jdbc.Driver");
                    Connection connection = DriverManager.getConnection(laConexion.url, laConexion.master, laConexion.masterkey);
                    PreparedStatement statement = connection.prepareStatement("INSERT INTO FOTO_CARGA(CARGA, FOTO) VALUES (?,?)");
                    statement.setString(1,strings[0]);
                    if(byteArray==null){
                        statement.setBinaryStream(2,null);
                    }else{
                        statement.setBinaryStream(2,new ByteArrayInputStream(byteArray),byteArray.length);
                    }
                    statement.executeUpdate();
                    exito=true;
                } catch (Exception e) {
                    exito= false;
                    msgError="ERRROR "+e.toString();
                    System.out.println(msgError);
                }
                return null;
            }
            @Override
            protected void onPostExecute(String aVoid) {
                if (MainActivity.this.pd != null) {
                    MainActivity.this.pd.dismiss();
                }
                if(exito){
                    mensajeCorto("Se subio la imagen de la carga "+carga);
                    limpiarTodo();
                }else{
                    mensajeCorto("EPIC FAIL => "+msgError);
                }
                super.onPostExecute(aVoid);
            }
        }


    private void showFileChooser() {
        ocultarTeclado();
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Imagen"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("ENTRA AL ACTIVITY REQUEST: ");
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            System.out.println("PICK PHOTO");
            Uri filePath = data.getData();
            try {
                //Cómo obtener el mapa de bits de la Galería
                double porcentajeCambio=1.0;
                imgBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                if(imgBitmap.getHeight()>1200){
                System.out.println("ALTURA ORIGINAL "+imgBitmap.getHeight());

                porcentajeCambio= (double) (1200.0/imgBitmap.getHeight());
                System.out.println("DELTA "+porcentajeCambio);
                }

                int ancho = (int) (imgBitmap.getWidth() * porcentajeCambio);
                int alto  = (int) (imgBitmap.getHeight() * porcentajeCambio);

                System.out.println("ANCHO "+ancho);
                System.out.println("ALTO "+alto);

                if(ancho>alto){
                    Matrix matrix = new Matrix(); matrix.postRotate(90);
                    Bitmap rotated = Bitmap.createBitmap(imgBitmap, 0, 0, imgBitmap.getWidth(), imgBitmap.getHeight(), matrix, true);
                    imageView.setImageBitmap(rotated);
                    Bitmap imageScaled = Bitmap.createScaledBitmap(rotated, alto, ancho, false);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    imageScaled.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byteArray = stream.toByteArray();
                    System.out.println("SUSUSU ENTRA AL ROTAR");
                }else{
                    imageView.setImageBitmap(imgBitmap);
                    Bitmap imageScaled = Bitmap.createScaledBitmap(imgBitmap, ancho, alto, false);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    imageScaled.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byteArray = stream.toByteArray();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
            System.out.println("TAKE PHOTO");
            try {
                //Cómo obtener el mapa de bits de la Galería
                double porcentajeCambio=1.0;
                imgBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoURI);
                if(imgBitmap.getHeight()>1200){
                    System.out.println("ALTURA ORIGINAL "+imgBitmap.getHeight());

                    porcentajeCambio= (double) (1200.0/imgBitmap.getHeight());
                    System.out.println("DELTA "+porcentajeCambio);
                }

                int ancho = (int) (imgBitmap.getWidth() * porcentajeCambio);
                int alto  = (int) (imgBitmap.getHeight() * porcentajeCambio);

                System.out.println("ANCHO "+ancho);
                System.out.println("ALTO "+alto);

                if(ancho>alto){
                    Matrix matrix = new Matrix(); matrix.postRotate(90);
                    Bitmap rotated = Bitmap.createBitmap(imgBitmap, 0, 0, imgBitmap.getWidth(), imgBitmap.getHeight(), matrix, true);
                    imageView.setImageBitmap(rotated);
                    Bitmap imageScaled = Bitmap.createScaledBitmap(rotated, alto, ancho, false);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    imageScaled.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byteArray = stream.toByteArray();
                    System.out.println("SUSUSU ENTRA AL ROTAR");
                }else{
                    imageView.setImageBitmap(imgBitmap);
                    Bitmap imageScaled = Bitmap.createScaledBitmap(imgBitmap, ancho, alto, false);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    imageScaled.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byteArray = stream.toByteArray();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void mensajeCorto(String mensaje){
        Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_LONG).show();
    }

    private void checkArchivosPermiso(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("Mensaje", "Permission not granted WRITE_EXTERNAL_STORAGE.");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        225);
            }
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("Mensaje", "Permission not granted CAMERA.");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        226);
            }
        }
    }

    private void abrirCamara(){
        ocultarTeclado();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "MyPicture");
                values.put(MediaStore.Images.Media.DESCRIPTION, "Photo taken on " + System.currentTimeMillis());
                photoURI = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                //Uri photoURI = FileProvider.getUriForFile(AddActivity.this, "com.example.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PHOTO);
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
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void ocultarTeclado(){
        try {
            View view = this.getCurrentFocus();
            view.clearFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }catch(NullPointerException ex){
            System.out.println(ex);
        }
    }

    public void limpiarTodo(){
        imageView.setImageBitmap(null);
        byteArray = null;
        editTextName.setText("");
    }
}