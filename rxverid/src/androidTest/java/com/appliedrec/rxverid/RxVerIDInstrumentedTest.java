package com.appliedrec.rxverid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import androidx.test.platform.app.InstrumentationRegistry;

import com.appliedrec.verid.core.Face;
import com.appliedrec.verid.core.FaceDetectionRecognitionFactory;
import com.appliedrec.verid.core.VerID;
import com.appliedrec.verid.identity.VerIDIdentity;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.Scheduler;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.schedulers.ExecutorScheduler;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class RxVerIDInstrumentedTest {

    public RxVerIDInstrumentedTest() {

    }

    @BeforeClass
    public static void setupRxSchedulers() {
        Scheduler immediate = new Scheduler() {
            @Override
            public Disposable scheduleDirect(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
                // this prevents StackOverflowErrors when scheduling with a delay
                return super.scheduleDirect(run, 0, unit);
            }

            @Override
            public Scheduler.Worker createWorker() {
                return new ExecutorScheduler.ExecutorWorker(Runnable::run, true);
            }
        };

        RxJavaPlugins.setInitIoSchedulerHandler(scheduler -> immediate);
        RxJavaPlugins.setInitComputationSchedulerHandler(scheduler -> immediate);
        RxJavaPlugins.setInitNewThreadSchedulerHandler(scheduler -> immediate);
        RxJavaPlugins.setInitSingleSchedulerHandler(scheduler -> immediate);
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(scheduler -> immediate);
    }

    Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private static final String VERID_PASSWORD = "d6994d96-3790-4bc4-b7ad-6b53a1e15c32";

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

    @Test
    public void test_getVerIDWithIdentity_returnsVerID() {
        try {
            InputStream inputStream = getContext().getAssets().open("Ver-ID identity.p12");
            VerIDIdentity identity = new VerIDIdentity(inputStream, VERID_PASSWORD);
            RxVerID rxVerID = new RxVerID.Builder(getContext()).setVerIDIdentity(identity).build();

            TestObserver<VerID> testObserver = rxVerID.getVerID().test();

            testObserver
                    .assertSubscribed()
                    .awaitDone(20, TimeUnit.SECONDS)
                    .assertNoErrors()
                    .assertComplete();
        } catch (Exception e) {
            getContext().getPackageName();
        }
    }

    @Test
    public void test_detectFaceInRemoteImage_succeeds() {
        try {
            RxVerID rxVerID = createRxVerID();
            TestObserver<Face> testObserver = rxVerID.detectFacesInImage(Uri.parse("https://ver-id.s3.us-east-1.amazonaws.com/test_images/jakub/Photo%2004-05-2016%2C%2018%2057%2050.jpg"), 1).test();

            testObserver
                    .assertSubscribed()
                    .assertValueCount(1)
                    .awaitDone(20, TimeUnit.SECONDS)
                    .assertNoErrors()
                    .assertComplete();
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void test_detectFaceInLocalImage_succeeds() {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("test", ".jpg");
            Uri localImageUri = Uri.fromFile(tempFile);
            RxVerID rxVerID = createRxVerID();

            URL imageURL = new URL("https://ver-id.s3.us-east-1.amazonaws.com/test_images/jakub/Photo%2004-05-2016%2C%2018%2057%2050.jpg");
            HttpsURLConnection connection = (HttpsURLConnection) imageURL.openConnection();
            connection.setDoInput(true);
            if (connection.getResponseCode() >= 400) {
                throw new Exception("Failed to download image");
            }
            try (InputStream imageInputStream = connection.getInputStream()) {
                try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                    int read;
                    byte[] buffer = new byte[256];
                    while ((read = imageInputStream.read(buffer, 0, buffer.length)) > 0) {
                        fileOutputStream.write(buffer, 0, read);
                    }
                    fileOutputStream.flush();

                    TestObserver<Face> testObserver = rxVerID.detectFacesInImage(localImageUri, 1).test();

                    testObserver
                            .assertSubscribed()
                            .assertValueCount(1)
                            .awaitDone(20, TimeUnit.SECONDS)
                            .assertNoErrors()
                            .assertComplete();
                }
            }
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        } finally {
            if (tempFile != null) {
                try {
                    tempFile.delete();
                } catch (Exception ignore) {}
            }
        }
    }

    private RxVerID createRxVerID() throws Exception {
        try (InputStream inputStream = getContext().getAssets().open("Ver-ID identity.p12")) {
            VerIDIdentity identity = new VerIDIdentity(inputStream, VERID_PASSWORD);
            return new RxVerID.Builder(getContext()).setVerIDIdentity(identity).build();
        }
    }
}
