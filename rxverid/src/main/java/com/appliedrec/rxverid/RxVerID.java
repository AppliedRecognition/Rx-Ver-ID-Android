package com.appliedrec.rxverid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.appliedrec.verid.core.Face;
import com.appliedrec.verid.core.IFaceDetectionFactory;
import com.appliedrec.verid.core.IFaceRecognitionFactory;
import com.appliedrec.verid.core.IUserManagementFactory;
import com.appliedrec.verid.core.ImageUtils;
import com.appliedrec.verid.core.RecognizableFace;
import com.appliedrec.verid.core.UserIdentification;
import com.appliedrec.verid.core.VerID;
import com.appliedrec.verid.core.VerIDFactory;
import com.appliedrec.verid.core.VerIDImage;

import org.javatuples.Pair;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;

public class RxVerID {

    // region Builder

    public static class Builder {

        static class Configuration extends Object {
            private Context context;
            private IFaceDetectionFactory faceDetectionFactory;
            private IFaceRecognitionFactory faceRecognitionFactory;
            private IUserManagementFactory userManagementFactory;

            public Context getContext() {
                return context;
            }

            public IFaceDetectionFactory getFaceDetectionFactory() {
                return faceDetectionFactory;
            }

            public void setFaceDetectionFactory(IFaceDetectionFactory faceDetectionFactory) {
                this.faceDetectionFactory = faceDetectionFactory;
            }

            public IFaceRecognitionFactory getFaceRecognitionFactory() {
                return faceRecognitionFactory;
            }

            public void setFaceRecognitionFactory(IFaceRecognitionFactory faceRecognitionFactory) {
                this.faceRecognitionFactory = faceRecognitionFactory;
            }

            public IUserManagementFactory getUserManagementFactory() {
                return userManagementFactory;
            }

            public void setUserManagementFactory(IUserManagementFactory userManagementFactory) {
                this.userManagementFactory = userManagementFactory;
            }

            @Override
            public boolean equals(@Nullable Object obj) {
                if (!(obj instanceof Configuration)) {
                    return false;
                }
                Configuration other = (Configuration)obj;
                return other.context == context && other.faceDetectionFactory == faceDetectionFactory && other.faceRecognitionFactory == faceRecognitionFactory && other.userManagementFactory == userManagementFactory;
            }
        }

        private static HashMap<Configuration,RxVerID> instances = new HashMap<>();

        private Configuration configuration = new Configuration();

        public Builder(Context context) {
            this.configuration.context = context;
        }

        Configuration getConfiguration() {
            return configuration;
        }

        public Builder setFaceDetectionFactory(IFaceDetectionFactory faceDetectionFactory) {
            getConfiguration().setFaceDetectionFactory(faceDetectionFactory);
            return this;
        }

        public Builder setFaceRecognitionFactory(IFaceRecognitionFactory faceRecognitionFactory) {
            getConfiguration().setFaceRecognitionFactory(faceRecognitionFactory);
            return this;
        }

        public Builder setUserManagementFactory(IUserManagementFactory userManagementFactory) {
            getConfiguration().setUserManagementFactory(userManagementFactory);
            return this;
        }

        public RxVerID build() {
            RxVerID rxVerID = getInstanceForConfiguration(getConfiguration());
            if (rxVerID == null) {
                rxVerID = new RxVerID(getConfiguration().getContext());
                rxVerID.faceDetectionFactory = getConfiguration().getFaceDetectionFactory();
                rxVerID.faceRecognitionFactory = getConfiguration().getFaceRecognitionFactory();
                rxVerID.userManagementFactory = getConfiguration().getUserManagementFactory();
                instances.put(getConfiguration(), rxVerID);
            }
            return rxVerID;
        }

        synchronized RxVerID getInstanceForConfiguration(Configuration configuration) {
            Iterator<Map.Entry<Configuration,RxVerID>> iterator = instances.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Configuration,RxVerID> entry = iterator.next();
                if (entry.getKey().equals(configuration)) {
                    return entry.getValue();
                }
            }
            return null;
        }
    }

    // endregion

    // region Properties

    private Context context;
    private VerID verID;
    private IFaceDetectionFactory faceDetectionFactory;
    private IFaceRecognitionFactory faceRecognitionFactory;
    private IUserManagementFactory userManagementFactory;
    private Object veridLock = new Object();

    // endregion

    // region Constructor (private – use Builder to create an instance)

    private RxVerID(@NonNull Context context) {
        this.context = context;
    }

    // endregion

    public Context getContext() {
        return context;
    }

    // region Factory getters

    public IFaceDetectionFactory getFaceDetectionFactory() {
        return faceDetectionFactory;
    }

    public IFaceRecognitionFactory getFaceRecognitionFactory() {
        return faceRecognitionFactory;
    }

    public IUserManagementFactory getUserManagementFactory() {
        return userManagementFactory;
    }

    // endregion

    VerIDFactory createVerIDFactory() {
        return new VerIDFactory(getContext(), null);
    }

    // region Ver-ID

    public Single<VerID> getVerID() {
        synchronized (veridLock) {
            if (verID != null) {
                return Single.just(verID);
            }
        }
        return Single.create(emitter -> {
            try {
                VerIDFactory verIDFactory = createVerIDFactory();
                if (getFaceDetectionFactory() != null) {
                    verIDFactory.setFaceDetectionFactory(getFaceDetectionFactory());
                }
                if (getFaceRecognitionFactory() != null) {
                    verIDFactory.setFaceRecognitionFactory(getFaceRecognitionFactory());
                }
                if (getUserManagementFactory() != null) {
                    verIDFactory.setUserManagementFactory(getUserManagementFactory());
                }
                VerID verID = verIDFactory.createVerIDSync();
                synchronized (veridLock) {
                    RxVerID.this.verID = verID;
                }
                if (!emitter.isDisposed()) {
                    emitter.onSuccess(verID);
                }
            } catch (Exception e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }
        });
    }

    // endregion

    // region Image conversion

    @IntDef({ExifInterface.ORIENTATION_NORMAL,ExifInterface.ORIENTATION_ROTATE_90,ExifInterface.ORIENTATION_ROTATE_180,ExifInterface.ORIENTATION_ROTATE_270,ExifInterface.ORIENTATION_FLIP_HORIZONTAL,ExifInterface.ORIENTATION_FLIP_VERTICAL,ExifInterface.ORIENTATION_TRANSPOSE,ExifInterface.ORIENTATION_TRANSVERSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ExifOrientation{};

    Single<ExifInterface> getExifFromUri(Uri uri) {
        return Single.create(emitter -> {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                ExifInterface exifInterface = new ExifInterface(inputStream);
                inputStream.close();
                emitter.onSuccess(exifInterface);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    Bitmap bitmapFromStream(InputStream inputStream) {
        return BitmapFactory.decodeStream(inputStream);
    }

    public Single<Integer> getExifOrientationOfImage(Uri imageUri) {
        return getExifFromUri(imageUri)
                .map(exifInterface -> exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
    }

    public Single<Bitmap> convertUriToBitmap(Uri imageUri) {
        return Single.create(emitter -> {
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = bitmapFromStream(inputStream);
                inputStream.close();
                emitter.onSuccess(bitmap);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    public Single<VerIDImage> convertUriToVerIDImage(Uri imageUri) {
        return convertUriToBitmap(imageUri)
                .flatMap(bitmap -> getExifOrientationOfImage(imageUri)
                        .flatMap(exifOrientation -> convertBitmapToVerIDImage(bitmap, exifOrientation)));
    }

    public Single<VerIDImage> convertBitmapToVerIDImage(Bitmap bitmap, @ExifOrientation int exifOrientation) {
        return Single.just(new VerIDImage(bitmap, exifOrientation));
    }

    // endregion

    // region Recognizable face detection

    public Observable<RecognizableFace> detectRecognizableFacesInImage(Uri imageUri, int limit) {
        return convertUriToVerIDImage(imageUri)
                .flatMapObservable(image -> detectRecognizableFacesInImage(image, limit));
    }

    public Observable<RecognizableFace> detectRecognizableFacesInImage(VerIDImage image, int limit) {
        return detectFacesInImage(image, limit)
                .flatMap(face -> convertFaceToRecognizableFace(image, face));
    }

    public Observable<RecognizableFace> detectRecognizableFacesInBitmap(Bitmap bitmap, @ExifOrientation int exifOrientation, int limit) {
        return convertBitmapToVerIDImage(bitmap, exifOrientation)
                .flatMapObservable(image -> detectRecognizableFacesInImage(image, limit));
    }

    public Observable<RecognizableFace> detectRecognizableFacesInBitmap(Bitmap bitmap, int limit) {
        return convertBitmapToVerIDImage(bitmap, ExifInterface.ORIENTATION_NORMAL)
                .flatMapObservable(image -> detectRecognizableFacesInImage(image, limit));
    }

    // endregion

    // region Face detection

    public Observable<Face> detectFacesInImage(Uri imageUri, int limit) {
        return convertUriToVerIDImage(imageUri)
                .flatMapObservable(image -> detectFacesInImage(image, limit));
    }

    public Observable<Face> detectFacesInImage(VerIDImage image, int limit) {
        return getVerID()
                .flatMapObservable(verID -> observer -> {
                    try {
                        Face[] faces = verID.getFaceDetection().detectFacesInImage(image, limit, 0);
                        for (Face face : faces) {
                            observer.onNext(face);
                        }
                        observer.onComplete();
                    } catch (Exception e) {
                        observer.onError(e);
                    }
                });
    }

    public Observable<Face> detectFacesInImage(Bitmap bitmap, int limit) {
        return detectFacesInImage(bitmap, ExifInterface.ORIENTATION_NORMAL, limit);
    }

    public Observable<Face> detectFacesInImage(Bitmap bitmap, @ExifOrientation int exifOrientation, int limit) {
        return convertBitmapToVerIDImage(bitmap, exifOrientation)
                .flatMapObservable(image -> detectFacesInImage(image, limit));
    }

    // endregion

    // region User identification

    public Single<UserIdentification> getUserIdentification() {
        return getVerID().map(UserIdentification::new);
    }

    public Observable<Pair<String,Float>> identifyUsersInImage(Uri imageUri) {
        return convertUriToVerIDImage(imageUri)
                .flatMapObservable(this::identifyUsersInImage);
    }

    public Observable<Pair<String,Float>> identifyUsersInImage(Bitmap bitmap) {
        return identifyUsersInImage(bitmap, ExifInterface.ORIENTATION_NORMAL);
    }

    public Observable<Pair<String,Float>> identifyUsersInImage(Bitmap bitmap, @ExifOrientation int exifOrientation) {
        return convertBitmapToVerIDImage(bitmap, exifOrientation)
                .flatMapObservable(this::identifyUsersInImage);
    }

    public Observable<Pair<String,Float>> identifyUsersInImage(VerIDImage image) {
        Observable<Pair<String,Float>> observable = getUserIdentification()
                .flatMapObservable(userIdentification -> detectRecognizableFacesInImage(image, 1)
                        .flatMap(face -> observer -> {
                            try {
                                Map<String,Float> userMap = userIdentification.identifyUsersInFace(face);
                                Iterator<Map.Entry<String,Float>> iterator = userMap.entrySet().iterator();
                                while (iterator.hasNext()) {
                                    Map.Entry<String,Float> entry = iterator.next();
                                    observer.onNext(new Pair<>(entry.getKey(), entry.getValue()));
                                }
                                observer.onComplete();
                            } catch (Exception e) {
                                observer.onError(e);
                            }
                        }));
        return observable.sorted((pair1, pair2) -> {
            if (pair1.getValue1() == pair2.getValue1()) {
                return pair1.getValue0().compareTo(pair2.getValue0());
            }
            return pair2.getValue1().compareTo(pair1.getValue1());
        });
    }

    // endregion

    // region Face to recognizable face conversion

    public Observable<RecognizableFace> convertFaceToRecognizableFace(VerIDImage image, Face face) {
        return getVerID()
                .toObservable()
                .flatMap(verID -> observer -> {
                    try {
                        RecognizableFace[] recognizableFaces = verID.getFaceRecognition().createRecognizableFacesFromFaces(new Face[]{face}, image);
                        if (recognizableFaces.length == 0) {
                            throw new Exception("Failed to create recognizable face");
                        }
                        for (RecognizableFace recognizableFace : recognizableFaces) {
                            observer.onNext(recognizableFace);
                        }
                        observer.onComplete();
                    } catch (Exception e) {
                        observer.onError(e);
                    }
                });
    }

    // endregion

    // region Cropping image to face

    Single<Matrix> getTransformMatrixForExifOrientation(@ExifOrientation int orientation) {
        return Single.just(ImageUtils.getMatrixFromExifOrientation(orientation));
    }

    public Single<Bitmap> cropImageToFace(@NonNull Bitmap bitmap, @NonNull Face face) {
        return Single.create(emitter -> {
            Rect cropRect = new Rect();
            face.getBounds().round(cropRect);
            cropRect.right = Math.min(bitmap.getWidth(), cropRect.right);
            cropRect.bottom = Math.min(bitmap.getHeight(), cropRect.bottom);
            cropRect.top = Math.max(0, cropRect.top);
            cropRect.left = Math.max(0, cropRect.left);
            Bitmap cropped = Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
            emitter.onSuccess(cropped);
        });
    }

    public Single<Bitmap> cropImageToFace(@NonNull Uri imageUri, @NonNull Face face) {
        return getExifOrientationOfImage(imageUri)
                .flatMap(this::getTransformMatrixForExifOrientation)
                .flatMap(matrix -> convertUriToBitmap(imageUri)
                        .map(bitmap -> {
                            if (matrix.isIdentity()) {
                                return bitmap;
                            }
                            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                        }))
                .flatMap(bitmap -> cropImageToFace(bitmap, face));
    }

    // endregion

    // region Session – in development

//    private <T extends VerIDSessionSettings> Observable<Pair<FaceDetectionResult,VerIDSessionResult>> session(T sessionSettings, IImageProviderServiceFactory imageProviderServiceFactory) {
//        return getVerID().flatMapObservable(verID -> {
//            IVideoEncoderServiceFactory videoEncoderServiceFactory;
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                videoEncoderServiceFactory = () -> {
//                    try {
//                        if (sessionSettings.shouldRecordSessionVideo()) {
//                            return new VideoEncoderService(File.createTempFile("video_", ".mp4"));
//                        }
//                    } catch (Exception e) {
//                    }
//                    return null;
//                };
//            } else {
//                videoEncoderServiceFactory = null;
//            }
//            return session(sessionSettings, imageProviderServiceFactory, new FaceDetectionServiceFactory(verID), new ResultEvaluationServiceFactory<>(verID), new ImageWriterServiceFactory(getContext()), videoEncoderServiceFactory);
//        });
//    }
//
//    private <T extends VerIDSessionSettings> Observable<Pair<FaceDetectionResult,VerIDSessionResult>> session(T sessionSettings, IImageProviderServiceFactory imageProviderServiceFactory, IFaceDetectionServiceFactory faceDetectionServiceFactory, IResultEvaluationServiceFactory<T> resultEvaluationServiceFactory, IImageWriterServiceFactory imageWriterServiceFactory, IVideoEncoderServiceFactory videoEncoderServiceFactory) {
//        try {
//            IImageProviderService imageProviderService = imageProviderServiceFactory.makeImageProviderService();
//            IFaceDetectionService faceDetectionService = faceDetectionServiceFactory.makeFaceDetectionService(sessionSettings);
//            IResultEvaluationService resultEvaluationService = resultEvaluationServiceFactory.makeResultEvaluationService(sessionSettings);
//            IImageWriterService imageWriterService = imageWriterServiceFactory.makeImageWriterService();
//            IVideoEncoderService videoEncoderService = videoEncoderServiceFactory.makeVideoEncoderService();
//            long expiryTime = System.currentTimeMillis() + sessionSettings.getExpiryTime();
//
//            Flowable<VerIDImage> imageFlowable = Flowable.create(emitter -> {
//                while (!emitter.isCancelled()) {
//                    if (System.currentTimeMillis() >= expiryTime) {
//                        emitter.onError(new TimeoutException("Session expired"));
//                        return;
//                    }
//                    try {
//                        emitter.onNext(imageProviderService.dequeueImage());
//                    } catch (Exception e) {
//                        emitter.onError(e);
//                        return;
//                    }
//                }
//            }, BackpressureStrategy.BUFFER);
//
//            Observable<VerIDImage> imageObservable = imageFlowable.map(image -> {
//                if (videoEncoderService != null) {
//                    if (!videoEncoderService.isEncodingStarted()) {
//                        videoEncoderService.startEncoding(image.getWidth(), image.getHeight(), imageProviderService.getOrientationOfCamera());
//                    }
//                    if (image.getYuvImage() != null) {
//                        videoEncoderService.queueYuvImage(image.getYuvImage());
//                    } else if (image.getBitmap() != null) {
//                        videoEncoderService.queueBitmap(image.getBitmap());
//                    }
//                }
//                return image;
//            })
//                    .onBackpressureLatest()
//                    .toObservable();
//
//            Observable<Pair<FaceDetectionResult,VerIDSessionResult>> resultObservable = imageObservable
//                    .map(image -> new Pair<>(image, faceDetectionService.detectFaceInImage(image)))
//                    .map(result -> {
//                        Uri uri;
//                        if (result.getValue1().getStatus() == FaceDetectionStatus.FACE_ALIGNED) {
//                            uri = imageWriterService.writeImage(result.getValue0());
//                        } else {
//                            uri = null;
//                        }
//                        ResultEvaluationStatus status = resultEvaluationService.addResult(result.getValue1(), result.getValue0(), uri);
//                        return new Triplet<>(result.getValue1(), resultEvaluationService.getSessionResult(), status);
//                    }).flatMap(triplet -> observer -> {
//                        Pair<FaceDetectionResult,VerIDSessionResult> pair = new Pair<>(triplet.getValue0(), triplet.getValue1());
//                        if (triplet.getValue2() == ResultEvaluationStatus.FINISHED) {
//                            if (triplet.getValue1().getError() != null) {
//                                observer.onError(triplet.getValue1().getError());
//                                return;
//                            }
//                            if (videoEncoderService != null) {
//                                videoEncoderService.setVideoEncodingListener(new VideoEncoderListener() {
//                                    @Override
//                                    public void onVideoEncoded(Uri uri) {
//                                        triplet.getValue1().setVideoUri(uri);
//                                        observer.onNext(pair);
//                                        observer.onComplete();
//                                    }
//
//                                    @Override
//                                    public void onVideoEncodingCancelled() {
//                                        observer.onNext(pair);
//                                        observer.onComplete();
//                                    }
//                                });
//                                videoEncoderService.stopEncoding();
//                            } else {
//                                observer.onNext(pair);
//                                observer.onComplete();
//                            }
//                        } else {
//                            observer.onNext(pair);
//                        }
//                    });
//
//            ReplaySubject<Pair<FaceDetectionResult,VerIDSessionResult>> replaySubject = ReplaySubject.create();
//            resultObservable.subscribe(replaySubject);
//            return replaySubject;
//        } catch (Exception e) {
//            return Observable.error(e);
//        }
//    }

    // endregion
}
