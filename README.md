![Maven metadata URL](https://img.shields.io/maven-metadata/v/https/dev.ver-id.com/artifactory/gradle-release/com/appliedrec/verid/rx/maven-metadata.xml.svg) ![Android CI](https://github.com/AppliedRecognition/Rx-Ver-ID-Android/workflows/Android%20CI/badge.svg)

# Rx-Ver-ID-Android

Reactive version of Ver-ID Core for Android

## Installation

1. [Register your app](https://dev.ver-id.com/licensing/). You will need your app's package name.
2. Registering your app will generate an evaluation licence for your app. The licence is valid for 30 days. If you need a production licence please [contact Applied Recognition](mailto:sales@appliedrec.com).
2. When you finish the registration you'll receive a file called **Ver-ID identity.p12** and a password. Copy the password to a secure location.
3. Copy the **Ver-ID identity.p12** into your app's assets folder. A common location is **your\_app_module/src/main/assets**.
8. Ver-ID will need the password you received at registration. Add the password in your app's **AndroidManifest.xml**:

    ~~~xml
    <manifest>
        <application>
            <meta-data
                android:name="com.appliedrec.verid.password"
                android:value="your password goes here" />
        </application>
    </manifest>
    ~~~
1. Add the Applied Recognition repository to the repositories in your app module's **gradle.build** file:

    ~~~groovy
    repositories {
      maven {
        url 'https://dev.ver-id.com/artifactory/gradle-release'
        name 'Ver-ID'
      }
    }
    ~~~  
1. Add the Rx-Ver-ID dependency in your app module's **gradle.build** file:

    ~~~groovy
    dependencies {
      implementation 'com.appliedrec.verid:rx:[1.6.0,2.0.0['
    }
    ~~~

## Examples

### Detect a face in an image and crop the image to the bounds of the face

~~~java
// Set this to an URI of an image
Uri imageUri;

// Image view to show the cropped image
ImageView imageView = findViewById(R.id.imageView);

// Create an instance of RxVerID
RxVerID rxVerID = new RxVerID.Builder(context).build();

rxVerID.detectFacesInImage(imageUri, 1) // Detect up to 1 face in the image URI
    .firstOrError() // Take the first detected face or throw and error if no face detected
    .flatMap(face -> rxVerID.cropImageToFace(imageUri, face)) // Crop the image to the face
    .subscribeOn(Schedulers.io()) // Subscribe on a background thread
    .observeOn(AndroidSchedulers.mainThread()) // Observe on main thread
    .subscribe(
        imageView::setImageBitmap, // Show the cropped bitmap in the image view
        error -> Toast.makeText(context, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show()
    );
~~~

### Detect a face in an image and assign it to a user

~~~java
// Set this to an URI of an image
Uri imageUri;

// Identifier for the user to whom the face will be assigned
String userId = "someUserId";

// Create an instance of RxVerID
RxVerID rxVerID = new RxVerID.Builder(context).build();

rxVerID.detectRecognizableFacesInImage(imageUri, 1) // Detect up to 1 face in the image URI
    .firstOrError() // Take the first detected face or throw and error if no face detected
    .flatMapCompletable(face -> rxVerID.assignFaceToUser(face, userId)) // Assign the detected face to user
    .subscribeOn(Schedulers.io()) // Subscribe on a background thread
    .observeOn(AndroidSchedulers.mainThread()) // Observe on main thread
    .subscribe(
        () -> Toast.makeText(context, "Face assigned to user "+userId, Toast.LENGTH_SHORT).show(),
        error -> Toast.makeText(context, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show()
    );
~~~

### Authenticate user in an image

~~~java
// Set this to an URI of an image
Uri imageUri;

// Identifier for the user who should be authenticated
String userId = "someUserId";

// Create an instance of RxVerID
RxVerID rxVerID = new RxVerID.Builder(context).build();

rxVerID.authenticateUserInImage(userId, imageUri) // Authenticate user in the image
    .subscribeOn(Schedulers.io()) // Subscribe on a background thread
    .observeOn(AndroidSchedulers.mainThread()) // Observe on main thread
    .subscribe(
        authenticated -> {
          String message;
          if (authenticated) {
            message = "User "+userId+" authenticated";
          } else {
            message = "Failed to authenticate user "+userId;
          }
          Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        },
        error -> Toast.makeText(context, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show()
    );
~~~

### Identify users in image

~~~java
// Set this to an URI of an image
Uri imageUri;

// Create an instance of RxVerID
RxVerID rxVerID = new RxVerID.Builder(context).build();

rxVerID.identifyUsersInImage(imageUri) // Identify users
    .toList() // Convert the observable to a list
    .subscribeOn(Schedulers.io()) // Subscribe on a background thread
    .observeOn(AndroidSchedulers.mainThread()) // Observe on main thread
    .subscribe(
      users -> {
        if (users.isEmpty()) {
          Toast.makeText(context, "No users identified in the image", Toast.LENGTH_SHORT).show();
          return;
        }
        ArrayList<String> names = new ArrayList<>();
        Iterator<Pair<String,Float>> iterator = users.iterator();
        while (iterator.hasNext()) {
          names.add(iterator.next().getValue0());
        }
        Toast.makeText(context, "Identified users: "+TextUtils.join(", ", names), Toast.LENGTH_SHORT).show();
      },
      error -> Toast.makeText(context, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show()
    );
~~~

### [Reference documentation](https://appliedrecognition.github.io/Rx-Ver-ID-Android/)
