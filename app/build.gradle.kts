plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.apollographql.apollo3") version "3.8.5"
}


android {
    namespace = "com.example.tripapp2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.tripapp2"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

apollo {
    service("service") {
        packageName.set("com.example.tripapp2.graphql")
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation("com.google.android.material:material:1.11.0")

    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // Activity & Fragment KTX (dla by viewModels())
    implementation("androidx.activity:activity-ktx:1.8.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // RecyclerView (jeśli jeszcze nie masz)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ConstraintLayout (jeśli jeszcze nie masz)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Coroutines (do async operacji)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
// Apollo GraphQL
    implementation("com.apollographql.apollo3:apollo-runtime:3.8.5")
    implementation("com.apollographql.apollo3:apollo-normalized-cache:3.8.5")

// OkHttp (transport + cookies)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}