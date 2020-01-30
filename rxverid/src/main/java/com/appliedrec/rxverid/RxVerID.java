package com.appliedrec.rxverid;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.appliedrec.verid.core.Bearing;
import com.appliedrec.verid.core.DetectedFace;
import com.appliedrec.verid.core.Face;
import com.appliedrec.verid.core.IFaceDetectionFactory;
import com.appliedrec.verid.core.IFaceRecognition;
import com.appliedrec.verid.core.IFaceRecognitionFactory;
import com.appliedrec.verid.core.IRecognizable;
import com.appliedrec.verid.core.IUserManagementFactory;
import com.appliedrec.verid.core.ImageUtils;
import com.appliedrec.verid.core.RecognizableFace;
import com.appliedrec.verid.core.UserIdentification;
import com.appliedrec.verid.core.VerID;
import com.appliedrec.verid.core.VerIDFactory;
import com.appliedrec.verid.core.VerIDImage;
import com.appliedrec.verid.core.VerIDSessionResult;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * Reactive implementation of common Ver-ID SDK tasks
 *
 * <p>Use {@link Builder Builder} to create an instance.</p>
 *
 * @since 1.0.0
 */
public class RxVerID {

    // region Builder

    /**
     * Builder class for RxVerID
     *
     * <p>Building an instance of {@link RxVerID RxVerID}:
     * <pre>
     * {@code new RxVerID.Builder(context).build();}
     * </pre>
     * </p>
     *
     * @since 1.0.0
     */
    public static class Builder {

        static class Configuration {
            private Context context;
            private IFaceDetectionFactory faceDetectionFactory;
            private IFaceRecognitionFactory faceRecognitionFactory;
            private IUserManagementFactory userManagementFactory;
            private String veridPassword;

            Context getContext() {
                return context;
            }

            IFaceDetectionFactory getFaceDetectionFactory() {
                return faceDetectionFactory;
            }

            void setFaceDetectionFactory(IFaceDetectionFactory faceDetectionFactory) {
                this.faceDetectionFactory = faceDetectionFactory;
            }

            IFaceRecognitionFactory getFaceRecognitionFactory() {
                return faceRecognitionFactory;
            }

            void setFaceRecognitionFactory(IFaceRecognitionFactory faceRecognitionFactory) {
                this.faceRecognitionFactory = faceRecognitionFactory;
            }

            IUserManagementFactory getUserManagementFactory() {
                return userManagementFactory;
            }

            void setUserManagementFactory(IUserManagementFactory userManagementFactory) {
                this.userManagementFactory = userManagementFactory;
            }

            String getVerIDPassword() {
                return veridPassword;
            }

            void setVerIDPassword(String veridPassword) {
                this.veridPassword = veridPassword;
            }

            @Override
            public boolean equals(@Nullable Object obj) {
                if (!(obj instanceof Configuration)) {
                    return false;
                }
                Configuration other = (Configuration)obj;
                return other.getContext() == getContext() && other.getFaceDetectionFactory() == getFaceRecognitionFactory() && other.getFaceRecognitionFactory() == getFaceRecognitionFactory() && other.getUserManagementFactory() == getUserManagementFactory();
            }
        }

        private static HashMap<Configuration,RxVerID> instances = new HashMap<>();

        private Configuration configuration = new Configuration();

        /**
         * Constructor
         * @param context Context
         * @since 1.0.0
         */
        public Builder(Context context) {
            this.configuration.context = context;
        }

        Configuration getConfiguration() {
            return configuration;
        }

        /**
         * Set face detection factory
         * @param faceDetectionFactory Instance of {@link IFaceDetectionFactory}
         * @return {@link Builder}
         * @since 1.0.0
         */
        public Builder setFaceDetectionFactory(IFaceDetectionFactory faceDetectionFactory) {
            getConfiguration().setFaceDetectionFactory(faceDetectionFactory);
            return this;
        }

        /**
         * Set face recognition factory
         * @param faceRecognitionFactory Instance of {@link IFaceRecognitionFactory}
         * @return {@link Builder}
         * @since 1.0.0
         */
        public Builder setFaceRecognitionFactory(IFaceRecognitionFactory faceRecognitionFactory) {
            getConfiguration().setFaceRecognitionFactory(faceRecognitionFactory);
            return this;
        }

        /**
         * Set user management factory
         * @param userManagementFactory Instance of {@link IUserManagementFactory}
         * @return {@link Builder}
         * @since 1.0.0
         */
        public Builder setUserManagementFactory(IUserManagementFactory userManagementFactory) {
            getConfiguration().setUserManagementFactory(userManagementFactory);
            return this;
        }

        /**
         * Set Ver-ID identity file password
         * @param veridPassword Ver-ID password
         * @return {@link Builder}
         * @since 1.7.0
         */
        public Builder setVerIDPassword(String veridPassword) {
            getConfiguration().setVerIDPassword(veridPassword);
            return this;
        }

        /**
         * Build an instance of {@link RxVerID}
         * @return Instance of {@link RxVerID}
         * @since 1.0.0
         */
        public RxVerID build() {
            RxVerID rxVerID = getInstanceForConfiguration(getConfiguration());
            if (rxVerID == null) {
                rxVerID = new RxVerID(getConfiguration().getContext());
                rxVerID.faceDetectionFactory = getConfiguration().getFaceDetectionFactory();
                rxVerID.faceRecognitionFactory = getConfiguration().getFaceRecognitionFactory();
                rxVerID.userManagementFactory = getConfiguration().getUserManagementFactory();
                rxVerID.veridPassword = getConfiguration().getVerIDPassword();
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
    private String veridPassword;
    private Object veridLock = new Object();

    // endregion

    // region Constructor (private – use Builder to create an instance)

    private RxVerID(@NonNull Context context) {
        this.context = context;
    }

    // endregion

    /**
     * Context used by this instance
     * @return Context
     * @since 1.0.0
     */
    public Context getContext() {
        return context;
    }

    // region Factory getters

    IFaceDetectionFactory getFaceDetectionFactory() {
        return faceDetectionFactory;
    }

    IFaceRecognitionFactory getFaceRecognitionFactory() {
        return faceRecognitionFactory;
    }

    IUserManagementFactory getUserManagementFactory() {
        return userManagementFactory;
    }

    // endregion

    VerIDFactory createVerIDFactory() {
        return new VerIDFactory(getContext());
    }

    // region Ver-ID

    /**
     * Get a Ver-ID instance
     * @return Single whose value is an instance of Ver-ID
     * @since 1.0.0
     */
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
                if (veridPassword != null) {
                    verIDFactory.setVeridPassword(veridPassword);
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
        }).subscribeOn(Schedulers.io()).cast(VerID.class);
    }

    // endregion

    // region Image conversion

    @IntDef({ExifInterface.ORIENTATION_NORMAL,ExifInterface.ORIENTATION_ROTATE_90,ExifInterface.ORIENTATION_ROTATE_180,ExifInterface.ORIENTATION_ROTATE_270,ExifInterface.ORIENTATION_FLIP_HORIZONTAL,ExifInterface.ORIENTATION_FLIP_VERTICAL,ExifInterface.ORIENTATION_TRANSPOSE,ExifInterface.ORIENTATION_TRANSVERSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ExifOrientation{};

    /**
     * Get EXIF tags in an image at the given URI
     * @param uri URI of the image
     * @return Single whose return value is an ExifInterface object
     * @since 1.0.0
     */
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

    /**
     * Decode a bitmap from an input stream
     * @param inputStream Input stream
     * @return Decoded bitmap
     * @since 1.0.0
     */
    Bitmap bitmapFromStream(InputStream inputStream) {
        return BitmapFactory.decodeStream(inputStream);
    }

    /**
     * Get EXIF orientation of an image at the given URI
     * @param imageUri URI of the image
     * @return Single whose value is an integer value of the EXIF orientation tag
     * @since 1.0.0
     */
    public Single<Integer> getExifOrientationOfImage(Uri imageUri) {
        return getExifFromUri(imageUri)
                .map(exifInterface -> exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
    }

    /**
     * Convert image at the given URI to a bitmap
     * @param imageUri URI of the image to convert
     * @return Single whose value is a bitmap
     * @since 1.0.0
     */
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

    /**
     * Convert image at the given URI to Ver-ID image
     * @param imageUri URI of the image to convert
     * @return Single whose value is a {@link VerIDImage Ver-ID image}
     * @since 1.0.0
     */
    public Single<VerIDImage> convertUriToVerIDImage(Uri imageUri) {
        return convertUriToBitmap(imageUri)
                .flatMap(bitmap -> getExifOrientationOfImage(imageUri)
                        .flatMap(exifOrientation -> convertBitmapToVerIDImage(bitmap, exifOrientation)));
    }

    /**
     * Convert bitmap to Ver-ID image
     * @param bitmap Bitmap to convert
     * @param exifOrientation EXIF orientation of the bitmap
     * @return Single whose value is a {@link VerIDImage Ver-ID image}
     * @since 1.0.0
     */
    public Single<VerIDImage> convertBitmapToVerIDImage(Bitmap bitmap, @ExifOrientation int exifOrientation) {
        return Single.just(new VerIDImage(bitmap, exifOrientation));
    }

    // endregion

    // region Recognizable face detection

    /**
     * Detect faces that can be used for face recognition in image
     * @param imageUri URI of the image in which to detect faces
     * @param limit Maximum number of faces to find
     * @return Observable whose values are recognizable faces detected in the image
     * @since 1.1.0
     */
    public Observable<RecognizableFace> detectRecognizableFacesInImage(Uri imageUri, int limit) {
        return convertUriToVerIDImage(imageUri)
                .flatMapObservable(image -> detectRecognizableFacesInImage(image, limit));
    }

    /**
     * Detect faces that can be used for face recognition in image
     * @param image Image in which to detect faces
     * @param limit Maximum number of faces to find
     * @return Observable whose values are recognizable faces detected in the image
     * @since 1.0.0
     */
    public Observable<RecognizableFace> detectRecognizableFacesInImage(VerIDImage image, int limit) {
        return detectFacesInImage(image, limit)
                .flatMap(face -> convertFaceToRecognizableFace(image, face));
    }

    /**
     * Detect faces that can be used for face recognition in image
     * @param bitmap Bitmap in which to detect faces
     * @param exifOrientation EXIF orientation of the bitmap
     * @param limit Maximum number of faces to find
     * @return Observable whose values are recognizable faces detected in the image
     * @since 1.0.0
     */
    public Observable<RecognizableFace> detectRecognizableFacesInImage(Bitmap bitmap, @ExifOrientation int exifOrientation, int limit) {
        return convertBitmapToVerIDImage(bitmap, exifOrientation)
                .flatMapObservable(image -> detectRecognizableFacesInImage(image, limit));
    }

    /**
     * Detect faces that can be used for face recognition in image
     * @param bitmap Bitmap in which to detect faces
     * @param limit Maximum number of faces to find
     * @return Observable whose values are recognizable faces detected in the image
     * @since 1.0.0
     */
    public Observable<RecognizableFace> detectRecognizableFacesInImage(Bitmap bitmap, int limit) {
        return convertBitmapToVerIDImage(bitmap, ExifInterface.ORIENTATION_NORMAL)
                .flatMapObservable(image -> detectRecognizableFacesInImage(image, limit));
    }

    /**
     * Detect faces that can be used for face recognition in image
     * @param verID Ver-ID instance
     * @param imageUri URI of the image in which to detect faces
     * @param limit Maximum number of faces to find
     * @return Observable whose values are recognizable faces detected in the image
     * @since 1.1.0
     */
    public Observable<RecognizableFace> detectRecognizableFacesInImage(VerID verID, Uri imageUri, int limit) {
        return convertUriToVerIDImage(imageUri)
                .flatMapObservable(image -> detectRecognizableFacesInImage(verID, image, limit));
    }

    /**
     * Detect faces that can be used for face recognition in image
     * @param verID Ver-ID instance
     * @param image Image in which to detect faces
     * @param limit Maximum number of faces to find
     * @return Observable whose values are recognizable faces detected in the image
     * @since 1.1.0
     */
    public Observable<RecognizableFace> detectRecognizableFacesInImage(VerID verID, VerIDImage image, int limit) {
        return detectFacesInImage(verID, image, limit)
                .flatMap(face -> convertFaceToRecognizableFace(verID, image, face));
    }

    /**
     * Detect faces that can be used for face recognition in image
     * @param verID Ver-ID instance
     * @param bitmap Bitmap in which to detect faces
     * @param exifOrientation EXIF orientation of the bitmap
     * @param limit Maximum number of faces to find
     * @return Observable whose values are recognizable faces detected in the image
     * @since 1.1.0
     */
    public Observable<RecognizableFace> detectRecognizableFacesInImage(VerID verID, Bitmap bitmap, @ExifOrientation int exifOrientation, int limit) {
        return convertBitmapToVerIDImage(bitmap, exifOrientation)
                .flatMapObservable(image -> detectRecognizableFacesInImage(verID, image, limit));
    }

    /**
     * Detect faces that can be used for face recognition in image
     * @param verID Ver-ID instance
     * @param bitmap Bitmap in which to detect faces
     * @param limit Maximum number of faces to find
     * @return Observable whose values are recognizable faces detected in the image
     * @since 1.1.0
     */
    public Observable<RecognizableFace> detectRecognizableFacesInImage(VerID verID, Bitmap bitmap, int limit) {
        return convertBitmapToVerIDImage(bitmap, ExifInterface.ORIENTATION_NORMAL)
                .flatMapObservable(image -> detectRecognizableFacesInImage(verID, image, limit));
    }

    // endregion

    // region Face detection

    /**
     * Detect faces in image
     * @param imageUri URI of the image in which to detect faces
     * @param limit Maximum number of faces to find
     * @return Observable whose values are faces detected in the image
     * @since 1.0.0
     */
    public Observable<Face> detectFacesInImage(Uri imageUri, int limit) {
        return convertUriToVerIDImage(imageUri)
                .flatMapObservable(image -> detectFacesInImage(image, limit));
    }

    /**
     * Detect faces in image
     * @param image Image in which to detect faces
     * @param limit Maximum number of faces to find
     * @return Observable whose values are faces detected in the image
     * @since 1.0.0
     */
    public Observable<Face> detectFacesInImage(VerIDImage image, int limit) {
        return getVerID()
                .flatMapObservable(verID -> detectFacesInImage(verID, image, limit));
    }

    /**
     * Detect faces in image
     * @param bitmap Bitmap in which to detect faces
     * @param limit Maximum number of faces to find
     * @return Observable whose values are faces detected in the image
     * @since 1.0.0
     */
    public Observable<Face> detectFacesInImage(Bitmap bitmap, int limit) {
        return detectFacesInImage(bitmap, ExifInterface.ORIENTATION_NORMAL, limit);
    }

    /**
     * Detect faces in image
     * @param bitmap Bitmap in which to detect faces
     * @param exifOrientation EXIF orientation of the bitmap
     * @param limit Maximum number of faces to find
     * @return Observable whose values are faces detected in the image
     * @since 1.0.0
     */
    public Observable<Face> detectFacesInImage(Bitmap bitmap, @ExifOrientation int exifOrientation, int limit) {
        return convertBitmapToVerIDImage(bitmap, exifOrientation)
                .flatMapObservable(image -> detectFacesInImage(image, limit));
    }

    /**
     * Detect faces in image
     * @param verID Ver-ID instance
     * @param imageUri URI of the image in which to detect faces
     * @param limit Maximum number of faces to find
     * @return Observable whose values are faces detected in the image
     * @since 1.1.0
     */
    public Observable<Face> detectFacesInImage(VerID verID, Uri imageUri, int limit) {
        return convertUriToVerIDImage(imageUri)
                .flatMapObservable(image -> detectFacesInImage(verID, image, limit));
    }

    /**
     * Detect faces in image
     * @param verID Ver-ID instance
     * @param image Image in which to detect faces
     * @param limit Maximum number of faces to find
     * @return Observable whose values are faces detected in the image
     * @since 1.1.0
     */
    public Observable<Face> detectFacesInImage(VerID verID, VerIDImage image, int limit) {
        return Observable.create(observer -> {
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

    /**
     * Detect faces in image
     * @param verID Ver-ID instance
     * @param bitmap Bitmap in which to detect faces
     * @param limit Maximum number of faces to find
     * @return Observable whose values are faces detected in the image
     * @since 1.1.0
     */
    public Observable<Face> detectFacesInImage(VerID verID, Bitmap bitmap, int limit) {
        return detectFacesInImage(verID, bitmap, ExifInterface.ORIENTATION_NORMAL, limit);
    }

    /**
     * Detect faces in image
     * @param verID Ver-ID instance
     * @param bitmap Bitmap in which to detect faces
     * @param exifOrientation EXIF orientation of the bitmap
     * @param limit Maximum number of faces to find
     * @return Observable whose values are faces detected in the image
     * @since 1.1.0
     */
    public Observable<Face> detectFacesInImage(VerID verID, Bitmap bitmap, @ExifOrientation int exifOrientation, int limit) {
        return convertBitmapToVerIDImage(bitmap, exifOrientation)
                .flatMapObservable(image -> detectFacesInImage(verID, image, limit));
    }

    // endregion

    // region User identification

    /**
     * Get user identification
     * @return Single whose value is an instance of UserIdentification
     * @since 1.0.0
     */
    public Single<UserIdentification> getUserIdentification() {
        return getVerID().map(UserIdentification::new);
    }

    /**
     * Identify users in an image
     * @param imageUri URI of the image in which to identify the users
     * @return Observable whose values are pairs of of user ID and score sorted by the best match
     * @since 1.0.0
     */
    public Observable<Pair<String,Float>> identifyUsersInImage(Uri imageUri) {
        return convertUriToVerIDImage(imageUri)
                .flatMapObservable(this::identifyUsersInImage);
    }

    /**
     * Identify users in an image
     * @param bitmap Bitmap in which to identify the users
     * @return Observable whose values are pairs of of user ID and score sorted by the best match
     * @since 1.0.0
     */
    public Observable<Pair<String,Float>> identifyUsersInImage(Bitmap bitmap) {
        return identifyUsersInImage(bitmap, ExifInterface.ORIENTATION_NORMAL);
    }

    /**
     * Identify users in an image
     * @param bitmap Bitmap in which to identify the users
     * @param exifOrientation EXIF orientation of the bitmap
     * @return Observable whose values are pairs of of user ID and score sorted by the best match
     * @since 1.0.0
     */
    public Observable<Pair<String,Float>> identifyUsersInImage(Bitmap bitmap, @ExifOrientation int exifOrientation) {
        return convertBitmapToVerIDImage(bitmap, exifOrientation)
                .flatMapObservable(this::identifyUsersInImage);
    }

    /**
     * Identify users in an image
     * @param image Image in which to identify the users
     * @return Observable whose values are pairs of of user ID and score sorted by the best match
     * @since 1.0.0
     */
    public Observable<Pair<String,Float>> identifyUsersInImage(VerIDImage image) {
        return getVerID()
                .flatMapObservable(verID -> identifyUsersInImage(verID, image));
    }

    /**
     * Get user identification
     * @param verID Ver-ID instance
     * @return Single whose value is an instance of UserIdentification
     * @since 1.1.0
     */
    public Single<UserIdentification> getUserIdentification(VerID verID) {
        return Single.just(new UserIdentification(verID));
    }

    /**
     * Identify users in an image
     * @param verID Ver-ID instance
     * @param imageUri URI of the image in which to identify the users
     * @return Observable whose values are pairs of of user ID and score sorted by the best match
     * @since 1.1.0
     */
    public Observable<Pair<String,Float>> identifyUsersInImage(VerID verID, Uri imageUri) {
        return convertUriToVerIDImage(imageUri)
                .flatMapObservable(image -> identifyUsersInImage(verID, image));
    }

    /**
     * Identify users in an image
     * @param verID Ver-ID instance
     * @param bitmap Bitmap in which to identify the users
     * @return Observable whose values are pairs of of user ID and score sorted by the best match
     * @since 1.1.0
     */
    public Observable<Pair<String,Float>> identifyUsersInImage(VerID verID, Bitmap bitmap) {
        return identifyUsersInImage(verID, bitmap, ExifInterface.ORIENTATION_NORMAL);
    }

    /**
     * Identify users in an image
     * @param verID Ver-ID instance
     * @param bitmap Bitmap in which to identify the users
     * @param exifOrientation EXIF orientation of the bitmap
     * @return Observable whose values are pairs of of user ID and score sorted by the best match
     * @since 1.1.0
     */
    public Observable<Pair<String,Float>> identifyUsersInImage(VerID verID, Bitmap bitmap, @ExifOrientation int exifOrientation) {
        return convertBitmapToVerIDImage(bitmap, exifOrientation)
                .flatMapObservable(image -> identifyUsersInImage(verID, image));
    }

    /**
     * Identify users in an image
     * @param verID Ver-ID instance
     * @param image Image in which to identify the users
     * @return Observable whose values are pairs of of user ID and score sorted by the best match
     * @since 1.1.0
     */
    public Observable<Pair<String,Float>> identifyUsersInImage(VerID verID, VerIDImage image) {
        return detectRecognizableFacesInImage(verID, image, 1)
                .flatMap(face -> identifyUsersInFace(verID, face));
    }

    /**
     * Identify users in a face
     * @param face Face in which to identify users
     * @return Observable whose values are pairs of of user ID and score sorted by the best match
     * @since 1.6.0
     */
    public Observable<Pair<String,Float>> identifyUsersInFace(RecognizableFace face) {
        return getVerID()
                .flatMapObservable(verID -> identifyUsersInFace(verID, face));
    }

    /**
     * Identify users in a face
     * @param verID Ver-ID instance
     * @param face Face in which to identify users
     * @return Observable whose values are pairs of of user ID and score sorted by the best match
     * @since 1.6.0
     */
    public Observable<Pair<String,Float>> identifyUsersInFace(VerID verID, RecognizableFace face) {
        Observable<Pair<String,Float>> observable = getUserIdentification(verID)
                .flatMapObservable(userIdentification -> observer -> {
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
                });
        return observable.sorted((pair1, pair2) -> {
            if (pair1.getValue1() == pair2.getValue1()) {
                return pair1.getValue0().compareTo(pair2.getValue0());
            }
            return pair2.getValue1().compareTo(pair1.getValue1());
        });
    }

    // endregion

    // region Face to recognizable face conversion

    /**
     * Convert a face detected in an image into a face that can be used for face recognition
     * @param image Image in which the face was detected
     * @param face Face to convert to recognizable face
     * @return Observable whose values are recognizable faces
     * @since 1.0.0
     */
    public Observable<RecognizableFace> convertFaceToRecognizableFace(VerIDImage image, Face face) {
        return getVerID()
                .flatMapObservable(verID -> convertFaceToRecognizableFace(verID, image, face));
    }

    /**
     * Convert a face detected in an image into a face that can be used for face recognition
     * @param verID Ver-ID instance
     * @param image Image in which the face was detected
     * @param face Face to convert to recognizable face
     * @return Observable whose values are recognizable faces
     * @since 1.1.0
     */
    public Observable<RecognizableFace> convertFaceToRecognizableFace(VerID verID, VerIDImage image, Face face) {
        return Observable.create(observer -> {
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

    /**
     * Transformation matrix that rights the image with the given EXIF orientation
     * @param orientation EXIF orientation
     * @return Single whose return value is a matrix
     * @since 1.0.0
     */
    Single<Matrix> getTransformMatrixForExifOrientation(@ExifOrientation int orientation) {
        return Single.just(ImageUtils.getMatrixFromExifOrientation(orientation));
    }

    /**
     * Crop bitmap to the bounds of a face
     * @param bitmap Bitmap to crop
     * @param face Face to whose bounds the image should be cropped
     * @return Single whose return value is a bitmap of the image cropped to the bounds of the face
     * @since 1.0.0
     */
    public Single<Bitmap> cropImageToFace(@NonNull Bitmap bitmap, @NonNull Face face) {
        return cropImageToFace(bitmap, ExifInterface.ORIENTATION_NORMAL, face);
    }

    /**
     * Crop bitmap to the bounds of a face
     * @param bitmap Bitmap to crop
     * @param exifOrientation EXIF orientation of the image
     * @param face Face to whose bounds the image should be cropped
     * @return Single whose return value is a bitmap of the image cropped to the bounds of the face
     * @since 1.4.0
     */
    public Single<Bitmap> cropImageToFace(@NonNull Bitmap bitmap, @ExifOrientation int exifOrientation, @NonNull Face face) {
        return correctBitmapOrientation(bitmap, exifOrientation)
                .map(rightedBitmap -> {
                    Rect cropRect = new Rect();
                    face.getBounds().round(cropRect);
                    cropRect.right = Math.min(rightedBitmap.getWidth(), cropRect.right);
                    cropRect.bottom = Math.min(rightedBitmap.getHeight(), cropRect.bottom);
                    cropRect.top = Math.max(0, cropRect.top);
                    cropRect.left = Math.max(0, cropRect.left);
                    Bitmap cropped = Bitmap.createBitmap(rightedBitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
                    return cropped;
                });
    }

    /**
     * Correct orientation of a bitmap using the value of EXIF orientation tag
     * @param bitmap Bitmap to correct
     * @param exifOrientation EXIF orientation tag value
     * @return Single whose return value is a bitmap oriented upright
     * @since 1.5.0
     */
    public Single<Bitmap> correctBitmapOrientation(@NonNull Bitmap bitmap, @ExifOrientation int exifOrientation) {
        return getTransformMatrixForExifOrientation(exifOrientation)
                .flatMap(matrix -> emitter -> {
                    if (!matrix.isIdentity()) {
                        Bitmap rightedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                        if (rightedBitmap == null) {
                            emitter.onError(new Exception("Unable to correct the bitmap orientation"));
                            return;
                        }
                        emitter.onSuccess(rightedBitmap);
                    } else {
                        emitter.onSuccess(bitmap);
                    }
                });
    }

    /**
     * Crop image to the bounds of a face
     * @param imageUri URI of the image to crop
     * @param face Face to whose bounds the image should be cropped
     * @return Single whose return value is a bitmap of the image cropped to the bounds of the face
     * @since 1.0.0
     */
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

    // region Face comparison

    /**
     * Compare face to faces
     * @param face Face to compare to other faces
     * @param faces Faces to compare to the face
     * @return Single whose return value is a face comparison score. If the score exceeds {@link IFaceRecognition#getAuthenticationThreshold() authentication threshold} the faces can be considered authenticated against the face.
     * @since 1.1.0
     * @see IFaceRecognition#getAuthenticationThreshold()
     */
    public Single<Float> compareFaceToFaces(IRecognizable face, RecognizableFace[] faces) {
        return getVerID()
                .flatMap(verID -> compareFaceToFaces(verID, face, faces));
    }

    /**
     * Compare face to faces
     * @param verID Ver-ID instance
     * @param face Face to compare to other faces
     * @param faces Faces to compare to the face
     * @return Single whose return value is a face comparison score. If the score exceeds {@link IFaceRecognition#getAuthenticationThreshold() authentication threshold} the faces can be considered authenticated against the face.
     * @since 1.1.0
     * @see IFaceRecognition#getAuthenticationThreshold()
     */
    public Single<Float> compareFaceToFaces(VerID verID, IRecognizable face, RecognizableFace[] faces) {
        return Single.create(emitter -> {
            try {
                float score = verID.getFaceRecognition().compareSubjectFacesToFaces(new IRecognizable[]{face}, faces);
                emitter.onSuccess(score);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    // endregion

    // region User management

    /**
     * Assign faces to user
     * @param faces Faces to assign to the user
     * @param user User to whom the faces should be assigned
     * @return Completable
     * @since 1.1.0
     */
    public Completable assignFacesToUser(IRecognizable[] faces, String user) {
        return getVerID()
                .flatMapCompletable(verID -> assignFacesToUser(verID, faces, user));
    }

    /**
     * Assign faces to user
     * @param verID Ver-ID instance
     * @param faces Faces to assign to the user
     * @param user User to whom the faces should be assigned
     * @return Completable
     * @since 1.1.0
     */
    public Completable assignFacesToUser(VerID verID, IRecognizable[] faces, String user) {
        return Completable.create(emitter -> {
            try {
                verID.getUserManagement().assignFacesToUser(faces, user);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    /**
     * Assign face to user
     * @param face Face to assign to the user
     * @param user User to whom the face should be assigned
     * @return Completable
     * @since 1.3.0
     */
    public Completable assignFaceToUser(IRecognizable face, String user) {
        return getVerID()
                .flatMapCompletable(verID -> assignFaceToUser(verID, face, user));
    }

    /**
     * Assign face to user
     * @param verID Ver-ID instance
     * @param face Face to assign to the user
     * @param user User to whom the face should be assigned
     * @return Completable
     * @since 1.3.0
     */
    public Completable assignFaceToUser(VerID verID, IRecognizable face, String user) {
        return Completable.create(emitter -> {
            try {
                verID.getUserManagement().assignFacesToUser(new IRecognizable[]{face}, user);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    /**
     * Delete a user
     * @param user Identifier of the user who is to be deleted
     * @return Completable
     * @since 1.1.0
     */
    public Completable deleteUser(String user) {
        return getVerID()
                .flatMapCompletable(verID -> deleteUser(verID, user));
    }

    /**
     * Delete a user
     * @param verID Ver-ID instance
     * @param user Identifier of the user who is to be deleted
     * @return Completable
     * @since 1.1.0
     */
    public Completable deleteUser(VerID verID, String user) {
        return Completable.create(emitter -> {
            try {
                verID.getUserManagement().deleteUsers(new String[]{user});
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    /**
     * Get users
     * @return Observable whose return values are identifiers of users who have at least one face registered
     * @since 1.1.0
     */
    public Observable<String> getUsers() {
        return getVerID()
                .flatMapObservable(RxVerID.this::getUsers);
    }

    /**
     * Get users
     * @param verID Ver-ID instance
     * @return Observable whose return values are identifiers of users who have at least one face registered
     * @since 1.1.0
     */
    public Observable<String> getUsers(VerID verID) {
        return Observable.create(emitter -> {
            try {
                String[] users = verID.getUserManagement().getUsers();
                Arrays.sort(users);
                for (String user : users) {
                    emitter.onNext(user);
                }
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    /**
     * Get faces of user
     * @param user Identifier for the user whose faces to get
     * @return Observable whose return value is a face of the user
     * @since 1.1.0
     */
    public Observable<IRecognizable> getFacesOfUser(String user) {
        return getVerID()
                .flatMapObservable(verID -> getFacesOfUser(verID, user));
    }

    /**
     * Get faces of user
     * @param verID Ver-ID instance
     * @param user Identifier for the user whose faces to get
     * @return Observable whose return value is a face of the user
     * @since 1.1.0
     */
    public Observable<IRecognizable> getFacesOfUser(VerID verID, String user) {
        return Observable.create(emitter -> {
            try {
                IRecognizable[] faces = verID.getUserManagement().getFacesOfUser(user);
                for (IRecognizable face : faces) {
                    emitter.onNext(face);
                }
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    // endregion

    // region User authentication

    /**
     * Authenticate user in faces
     * @param user Identifier for the user to be authenticated
     * @param faces Faces to authenticate as the user
     * @return Single whose return value indicates whether the user was authenticated {@literal true} or not {@literal false}
     * @since 1.1.0
     */
    public Single<Boolean> authenticateUserInFaces(String user, RecognizableFace[] faces) {
        return getVerID()
                .flatMap(verID -> authenticateUserInFaces(verID, user, faces));
    }

    /**
     * Authenticate user in faces
     * @param verID Ver-ID instance
     * @param user Identifier for the user to be authenticated
     * @param faces Faces to authenticate as the user
     * @return Single whose return value indicates whether the user was authenticated {@literal true} or not {@literal false}
     * @since 1.1.0
     */
    public Single<Boolean> authenticateUserInFaces(VerID verID, String user, RecognizableFace[] faces) {
        return Single.create(emitter -> {
            try {
                IRecognizable[] userFaces = verID.getUserManagement().getFacesOfUser(user);
                float score = verID.getFaceRecognition().compareSubjectFacesToFaces(userFaces, faces);
                emitter.onSuccess(score >= verID.getFaceRecognition().getAuthenticationThreshold());
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    /**
     * Authenticate user in face
     * @param user User to authenticate
     * @param face Face in which to authenticate the user
     * @return Single whose return value indicates whether the user was authenticated {@literal true} or not {@literal false}
     * @since 1.6.0
     */
    public Single<Boolean> authenticateUserInFace(String user, RecognizableFace face) {
        return getVerID()
                .flatMap(verID -> authenticateUserInFace(verID, user, face));
    }

    /**
     * Authenticate user in face
     * @param verID Ver-ID instance
     * @param user User to authenticate
     * @param face Face in which to authenticate the user
     * @return Single whose return value indicates whether the user was authenticated {@literal true} or not {@literal false}
     * @since 1.6.0
     */
    public Single<Boolean> authenticateUserInFace(VerID verID, String user, RecognizableFace face) {
        return authenticateUserInFaces(verID, user, new RecognizableFace[]{face});
    }

    /**
     * Authenticate user in image
     * @param user User to authenticate
     * @param image Image in which to authenticate the user
     * @return Single whose return value indicates whether the user was authenticated {@literal true} or not {@literal false}
     * @since 1.6.0
     */
    public Single<Boolean> authenticateUserInImage(String user, Uri image) {
        return getVerID()
                .flatMap(verID -> authenticateUserInImage(verID, user, image));
    }

    /**
     * Authenticate user in image
     * @param verID Ver-ID instance
     * @param user User to authenticate
     * @param image Image in which to authenticate the user
     * @return Single whose return value indicates whether the user was authenticated {@literal true} or not {@literal false}
     * @since 1.6.0
     */
    public Single<Boolean> authenticateUserInImage(VerID verID, String user, Uri image) {
        return detectRecognizableFacesInImage(verID, image, 4)
                .flatMap(face -> authenticateUserInFace(verID, user, face).toObservable())
                .filter(authenticated -> authenticated)
                .first(false);
    }

    /**
     * Authenticate user in image
     * @param user User to authenticate
     * @param image Image in which to authenticate the user
     * @return Single whose return value indicates whether the user was authenticated {@literal true} or not {@literal false}
     * @since 1.6.0
     */
    public Single<Boolean> authenticateUserInImage(String user, VerIDImage image) {
        return getVerID()
                .flatMap(verID -> authenticateUserInImage(verID, user, image));
    }

    /**
     * Authenticate user in image
     * @param verID Ver-ID instance
     * @param user User to authenticate
     * @param image Image in which to authenticate the user
     * @return Single whose return value indicates whether the user was authenticated {@literal true} or not {@literal false}
     * @since 1.6.0
     */
    public Single<Boolean> authenticateUserInImage(VerID verID, String user, VerIDImage image) {
        return detectRecognizableFacesInImage(verID, image, 4)
                .flatMap(face -> authenticateUserInFace(verID, user, face).toObservable())
                .filter(authenticated -> authenticated)
                .first(false);
    }

    /**
     * Authenticate user in image
     * @param user User to authenticate
     * @param image Image in which to authenticate the user
     * @return Single whose return value indicates whether the user was authenticated {@literal true} or not {@literal false}
     * @since 1.6.0
     */
    public Single<Boolean> authenticateUserInImage(String user, Bitmap image) {
        return getVerID()
                .flatMap(verID -> authenticateUserInImage(verID, user, image));
    }

    /**
     * Authenticate user in image
     * @param verID Ver-ID instance
     * @param user User to authenticate
     * @param image Image in which to authenticate the user
     * @return Single whose return value indicates whether the user was authenticated {@literal true} or not {@literal false}
     * @since 1.6.0
     */
    public Single<Boolean> authenticateUserInImage(VerID verID, String user, Bitmap image) {
        return authenticateUserInImage(verID, user, image, ExifInterface.ORIENTATION_NORMAL);
    }

    /**
     * Authenticate user in image
     * @param user User to authenticate
     * @param image Image in which to authenticate the user
     * @param exifOrientation EXIF orientation of the bitmap
     * @return Single whose return value indicates whether the user was authenticated {@literal true} or not {@literal false}
     * @since 1.6.0
     */
    public Single<Boolean> authenticateUserInImage(String user, Bitmap image, @ExifOrientation int exifOrientation) {
        return getVerID()
                .flatMap(verID -> authenticateUserInImage(verID, user, image, exifOrientation));
    }

    /**
     * Authenticate user in image
     * @param verID Ver-ID instance
     * @param user User to authenticate
     * @param image Image in which to authenticate the user
     * @param exifOrientation EXIF orientation of the bitmap
     * @return Single whose return value indicates whether the user was authenticated {@literal true} or not {@literal false}
     * @since 1.6.0
     */
    public Single<Boolean> authenticateUserInImage(VerID verID, String user, Bitmap image, @ExifOrientation int exifOrientation) {
        return detectRecognizableFacesInImage(verID, image, exifOrientation,4)
                .flatMap(face -> authenticateUserInFace(verID, user, face).toObservable())
                .filter(authenticated -> authenticated)
                .first(false);
    }

    // endregion

    // region Session result parsing

    /**
     * Get session result from intent
     * @param intent Intent from which to extract session result
     * @return Single whose value is a successful session result
     * @since 1.2.0
     */
    public Single<VerIDSessionResult> getSessionResultFromIntent(Intent intent) {
        return Single.create(emitter -> {
            if (intent == null) {
                emitter.onError(new NullPointerException());
                return;
            }
            VerIDSessionResult result = intent.getParcelableExtra("com.appliedrec.verid.ui.EXTRA_RESULT");
            if (result == null) {
                emitter.onError(new Exception("Failed to parse Ver-ID session result from intent"));
                return;
            }
            if (result.getError() != null) {
                emitter.onError(result.getError());
                return;
            }
            emitter.onSuccess(result);
        });
    }

    /**
     * Get image URI, face and bearing from session result
     * @param result Session result from which to extract the URI, face and bearing
     * @return Observable whose values are {@link Triplet triplets} of {@link Uri}, {@link Face} and {@link Bearing}
     * @since 1.2.0
     */
    public Observable<Triplet<Uri, Face, Bearing>> getImageUriFaceAndBearingFromSessionResult(VerIDSessionResult result) {
        return Observable.create(emitter -> {
            if (result.getError() != null) {
                emitter.onError(result.getError());
                return;
            }
            for (DetectedFace detectedFace : result.getAttachments()) {
                if (detectedFace.getFace() != null && detectedFace.getImageUri() != null) {
                    emitter.onNext(new Triplet<>(detectedFace.getImageUri(), detectedFace.getFace(), detectedFace.getBearing()));
                }
            }
            emitter.onComplete();
        });
    }

    /**
     * Get faces and image URIs from a session result
     * @param result Session result
     * @param bearing Limit the results to a bearing or {@literal null} to include all bearings
     * @return Observable whose values are {@link DetectedFace} objects
     * @since 1.3.0
     */
    public Observable<DetectedFace> getFacesAndImageUrisFromSessionResult(VerIDSessionResult result, @Nullable Bearing bearing) {
        return Observable.create(emitter -> {
            if (result.getError() != null) {
                emitter.onError(result.getError());
                return;
            }
            for (DetectedFace detectedFace : result.getAttachments()) {
                if (detectedFace.getFace() != null && detectedFace.getImageUri() != null && (bearing == null || bearing == detectedFace.getBearing())) {
                    emitter.onNext(detectedFace);
                }
            }
            emitter.onComplete();
        });
    }

    /**
     * Get faces and image URIs from a session result
     * @param result Session result
     * @return Observable whose values are {@link DetectedFace} objects
     * @since 1.3.0
     */
    public Observable<DetectedFace> getFacesAndImageUrisFromSessionResult(VerIDSessionResult result) {
        return getFacesAndImageUrisFromSessionResult(result, null);
    }

    /**
     * Get faces that can be used for face recognition from a session result
     * @param result Session result
     * @param bearing Limit the result to faces with the given bearing
     * @return Observable whose values are faces that can be used for face recognition
     * @since 1.3.0
     */
    public Observable<RecognizableFace> getRecognizableFacesFromSessionResult(VerIDSessionResult result, @Nullable Bearing bearing) {
        return Observable.create(emitter -> {
            if (result.getError() != null) {
                emitter.onError(result.getError());
                return;
            }
            for (DetectedFace detectedFace : result.getAttachments()) {
                if (detectedFace.getFace() != null && detectedFace.getFace() instanceof RecognizableFace && (bearing == null || detectedFace.getBearing() == bearing)) {
                    emitter.onNext((RecognizableFace) detectedFace.getFace());
                }
            }
            emitter.onComplete();
        });
    }

    /**
     * Get faces that can be used for face recognition from a session result
     * @param result Session result
     * @return Observable whose values are faces that can be used for face recognition
     * @since 1.3.0
     */
    public Observable<RecognizableFace> getRecognizableFacesFromSessionResult(VerIDSessionResult result) {
        return getRecognizableFacesFromSessionResult(result, null);
    }

    /**
     * Get image URIs from a session result
     * @param result Session result
     * @param bearing Limit the images to faces with the given bearing
     * @return Observable whose values are URIs of the images captured during a successful Ver-ID session
     * @since 1.3.0
     */
    public Observable<Uri> getImageUrisFromSessionResult(VerIDSessionResult result, @Nullable Bearing bearing) {
        return Observable.create(emitter -> {
            if (result.getError() != null) {
                emitter.onError(result.getError());
                return;
            }
            for (DetectedFace detectedFace : result.getAttachments()) {
                if (detectedFace.getImageUri() != null && (bearing == null || detectedFace.getBearing() == bearing)) {
                    emitter.onNext(detectedFace.getImageUri());
                }
            }
            emitter.onComplete();
        });
    }

    /**
     * Get image URIs from a session result
     * @param result Session result
     * @return Observable whose values are URIs of the images captured during a successful Ver-ID session
     * @since 1.3.0
     */
    public Observable<Uri> getImageUrisFromSessionResult(VerIDSessionResult result) {
        return getImageUrisFromSessionResult(result, null);
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
