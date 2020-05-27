package com.darkabhi.pdfthumbnail;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public final static String FOLDER = Environment.getExternalStorageDirectory() + "/PDF";

    private int PICK_PDF_REQUEST = 10;

    private Uri filePath;

    private Button selectPDFButton, viewPDFButton;

    private TextView filenameTextView, countTextView, folderPath;

    private ImageView iv;

    private String convertedFileName;
    private String pageCount;

    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectPDFButton = findViewById(R.id.button);
        viewPDFButton = findViewById(R.id.viewPDF);

        filenameTextView = findViewById(R.id.filename);
        countTextView = findViewById(R.id.count);
        folderPath= findViewById(R.id.folder_path);

        iv = findViewById(R.id.imageView);

        folderPath.setText(FOLDER);

        selectPDF();
        viewPDF();
    }

    private void viewPDF() {
        viewPDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPDF();
            }
        });
    }

    private void selectPDF() {
        selectPDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileChooser();
            }
        });
    }

    void generateImageFromPdf(Uri pdfUri) {
        int pageNumber = 0;
        PdfiumCore pdfiumCore = new PdfiumCore(this);
        try {
            ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(pdfUri, "r");
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
            pdfiumCore.openPage(pdfDocument, pageNumber);
            pageCount = String.valueOf(pdfiumCore.getPageCount(pdfDocument));
            int width = pdfiumCore.getPageWidth(pdfDocument, pageNumber);
            int height = pdfiumCore.getPageHeight(pdfDocument, pageNumber);
            Log.e(TAG,"\nPDFURI: "+pdfUri+"\n"+"fd: "+fd+"\nPDFDocument "+pdfDocument+ "\nWidth & Height: "+width+"\t"+height);
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
            saveImage(bmp);
            iv.setImageBitmap(bmp);
            pdfiumCore.closeDocument(pdfDocument); // important!
        } catch (Exception e) {
            //todo with exception
        }
    }


    private void fileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_PDF_REQUEST);
        setResult(Activity.RESULT_OK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            convertedFileName = getFileName(filePath);
            generateImageFromPdf(filePath);
            filenameTextView.setText(convertedFileName);
            countTextView.setText(pageCount);
        }
    }

    private void openPDF() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(filePath, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void saveImage(Bitmap bmp) {
        Log.e(TAG, String.valueOf(bmp));
        FileOutputStream out = null;
        try {
            File folder = new File(FOLDER);
            if (!folder.exists())
                folder.mkdirs();
            File file = new File(folder, "PDF.png");
            out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
        } catch (Exception e) {
            //todo with exception
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (Exception e) {
                //todo with exception
            }
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
