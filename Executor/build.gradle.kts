plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.json:json:20240303")
    // Google API Client
    implementation("com.google.api-client:google-api-client:2.2.0")
// Google OAuth Client Jetty for local server auth
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
// Google Calendar API Service
    implementation("com.google.apis:google-api-services-calendar:v3-rev20220715-2.0.0")
    // CheckiDay
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    systemProperty("java.library.path", "/opt/homebrew/opt/libusb/lib")
}


