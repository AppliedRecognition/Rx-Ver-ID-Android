package com.appliedrec.rxverid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;
import androidx.test.platform.app.InstrumentationRegistry;

import com.appliedrec.verid.core.FaceDetectionRecognitionFactory;
import com.appliedrec.verid.core.VerID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class RxVerIDTest {

    Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void test_bitmapFromStream_returnsBitmap() {
        int[] pixels = new int[]{Color.RED, Color.GREEN, Color.BLUE, Color.BLACK};
        Bitmap originalBitmap = Bitmap.createBitmap(pixels, 2, 2, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] png = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(png);
        RxVerID rxVerID = new RxVerID.Builder(getContext()).build();
        Bitmap bitmap = rxVerID.bitmapFromStream(inputStream);
        assertEquals(bitmap.getPixel(0,0), Color.RED);
        assertEquals(bitmap.getPixel(1,0), Color.GREEN);
        assertEquals(bitmap.getPixel(0,1), Color.BLUE);
        assertEquals(bitmap.getPixel(1,1), Color.BLACK);
    }

    @Test
    public void test_getExifFromUri_returnsExif() {
        File file = null;
        try {
            int[] pixels = new int[]{Color.RED, Color.GREEN, Color.BLUE, Color.BLACK};
            Bitmap originalBitmap = Bitmap.createBitmap(pixels, 2, 2, Bitmap.Config.ARGB_8888);
            file = File.createTempFile("test_",".png");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            Uri uri = Uri.fromFile(file);
            RxVerID rxVerID = new RxVerID.Builder(getContext()).build();

            TestObserver<ExifInterface> testObserver = rxVerID.getExifFromUri(uri).test();

            testObserver
                    .assertSubscribed()
                    .assertNoErrors()
                    .assertComplete();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    @Test
    public void test_getVerID_returnsVerID() {
        FaceDetectionRecognitionFactory faceDetectionRecognitionFactory = new FaceDetectionRecognitionFactory(getContext(), "c26d437415feeb9122eb72fd262e262d46f068562a5f93a78baae730df82dde3");

        RxVerID rxVerID = new RxVerID.Builder(getContext()).setFaceDetectionFactory(faceDetectionRecognitionFactory).setFaceRecognitionFactory(faceDetectionRecognitionFactory).build();

        TestObserver<VerID> testObserver = rxVerID.getVerID().test();

        testObserver
                .assertSubscribed()
                .awaitDone(20, TimeUnit.SECONDS)
                .assertNoErrors()
                .assertComplete();
    }
}
