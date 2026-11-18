plugins {
    alias(libs.plugins.convention.android.application)
    alias(libs.plugins.convention.android.application.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// 서명 키는 저장소에 두지 않고 CI(release.yml)가 Secrets에서 복원해 환경변수로 넘긴다.
// 로컬처럼 값이 없으면 서명 없는 release APK가 만들어진다.
val releaseKeystorePath: String? = providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull

android {
    namespace = "com.mg.pillreminder"

    defaultConfig {
        applicationId = "com.mg.pillreminder"
        // release.yml이 -PversionCode=<run 번호> -PversionName=<태그>로 넘긴다.
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            // packageScope로 범위를 나누면 R8이 kotlin.collections의 package-private
            // multifile 파트(CollectionsKt__IteratorsKt)만 다른 패키지로 옮겨 부모 접근이 깨지고
            // 시작 시 IllegalAccessError로 죽었다(v0.2.0). 앱 전체를 한 기준으로 최적화한다.
            optimization {
                enable = true
            }
        }
    }
}

room {
    // 2차에 DoseRecord를 추가할 때 마이그레이션을 검증하려고 스키마를 커밋한다.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
