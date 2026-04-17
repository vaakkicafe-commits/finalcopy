import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
    // alias(libs.plugins.hilt)
    // alias(libs.plugins.ksp)
}

android {
    namespace = "com.leevaakki.cafe"
    compileSdk = 35

    val props = Properties()
    val propFile = rootProject.file("local.properties")
    if (propFile.exists()) {
        val inputStream = propFile.inputStream()
        props.load(inputStream)
        inputStream.close()
    }

    defaultConfig {
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
        manifestPlaceholders["deepLinkHost"] = "leevaakki.com"
        manifestPlaceholders["RAZORPAY_KEY"] = "rzp_test_placeholder"
    }

    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true
    }

    signingConfigs {
        create("release") {
            val storePath = "/Users/vaakki/lee_vaakki_key.jks"
            val storePw = props.getProperty("RELEASE_STORE_PASSWORD")
            val alias = props.getProperty("RELEASE_KEY_ALIAS")
            val keyPw = props.getProperty("RELEASE_KEY_PASSWORD")

            if (storePw != null && alias != null && keyPw != null && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = storePw
                keyAlias = alias
                keyPassword = keyPw
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isDebuggable = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
        }
    }

    kotlinOptions {
        jvmTarget = "17"
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    flavorDimensions += "brand"
    productFlavors {
        create("cafe") {
            dimension = "brand"
            applicationId = "com.leevaakki.cafe"
            versionNameSuffix = "-cafe"
            buildConfigField("String", "BRANCH_TYPE", "\"CAFE\"")
            buildConfigField("String", "SWIGGY_URL", "\"https://www.swiggy.com/menu/886071\"")
            buildConfigField("String", "ZOMATO_URL", "\"https://www.zomato.com/ncr/vaakki-cafe-kamla-nagar-new-delhi/order\"")
            buildConfigField("String", "MAPS_URL", "\"https://maps.app.goo.gl/3f4Y2E9U5v8x7XvR9\"")
            // PROD: Use your live domain
            buildConfigField("String", "WORDPRESS_URL", "\"https://leevaakkicafe.com\"")
            buildConfigField("String", "DEEP_LINK_HOST", "\"leevaakkicafe.com\"")
            manifestPlaceholders["deepLinkHost"] = "leevaakkicafe.com"
            val razorpayKey = props.getProperty("RAZORPAY_KEY_CAFE") ?: ""
            buildConfigField("String", "RAZORPAY_KEY", "\"$razorpayKey\"")
            manifestPlaceholders["RAZORPAY_KEY"] = razorpayKey
        }
        create("dhaba") {
            dimension = "brand"
            applicationId = "com.leevaakki.dhaba"
            versionNameSuffix = "-dhaba"
            buildConfigField("String", "BRANCH_TYPE", "\"DHABA\"")
            buildConfigField("String", "SWIGGY_URL", "\"https://www.swiggy.com/menu/dhaba_placeholder\"")
            buildConfigField("String", "ZOMATO_URL", "\"https://www.zomato.com/ncr/vaakki-dhaba-order\"")
            buildConfigField("String", "MAPS_URL", "\"https://maps.google.com/?q=Vaakki+Dhaba\"")
            // PROD: Use your live domain
            buildConfigField("String", "WORDPRESS_URL", "\"https://leevaakkidhaba.com\"")
            buildConfigField("String", "DEEP_LINK_HOST", "\"leevaakkidhaba.com\"")
            manifestPlaceholders["deepLinkHost"] = "leevaakkidhaba.com"
            val razorpayKey = props.getProperty("RAZORPAY_KEY_DHABA") ?: ""
            buildConfigField("String", "RAZORPAY_KEY", "\"$razorpayKey\"")
            manifestPlaceholders["RAZORPAY_KEY"] = razorpayKey
        }
        create("farm") {
            dimension = "brand"
            applicationId = "com.leevaakki.farm"
            versionNameSuffix = "-farm"
            buildConfigField("String", "BRANCH_TYPE", "\"FARM\"")
            buildConfigField("String", "SWIGGY_URL", "\"https://www.swiggy.com/menu/farm_placeholder\"")
            buildConfigField("String", "ZOMATO_URL", "\"https://www.zomato.com/ncr/vaakki-farm-order\"")
            buildConfigField("String", "MAPS_URL", "\"https://maps.google.com/?q=Vaakki+Farm\"")
            // PROD: Use your live domain
            buildConfigField("String", "WORDPRESS_URL", "\"https://leevaakkipvtld.com\"")
            buildConfigField("String", "DEEP_LINK_HOST", "\"leevaakkipvtld.com\"")
            manifestPlaceholders["deepLinkHost"] = "leevaakkipvtld.com"
            val razorpayKey = props.getProperty("RAZORPAY_KEY_FARM") ?: ""
            buildConfigField("String", "RAZORPAY_KEY", "\"$razorpayKey\"")
            manifestPlaceholders["RAZORPAY_KEY"] = razorpayKey
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)
    implementation(libs.coil.compose)
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation("com.razorpay:checkout:1.6.41")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    
    // implementation(libs.hilt.android)
    // ksp(libs.hilt.compiler)
    // implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
