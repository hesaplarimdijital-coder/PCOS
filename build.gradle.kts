// Kök derleme dosyası. Modüllerin ortak eklenti sürümleri burada "apply false" ile bildirilir;
// asıl yapılandırma app/ ve core/cycle/ altındadır.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}
