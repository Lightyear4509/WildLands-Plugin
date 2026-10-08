plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}
group = "gg.ggwp.wildlands"
version = "0.9.0"
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
val mockitoAgent by configurations.creating
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0") { exclude(group = "org.slf4j") }
    testImplementation("io.papermc.paper:paper-api:26.2.build.121-stable")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.23.0")
    mockitoAgent("org.mockito:mockito-core:5.23.0") { isTransitive = false }
}
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
tasks.withType<JavaCompile>().configureEach { options.release.set(25); options.encoding = "UTF-8" }
val pluginVersion = project.version.toString()
tasks.processResources {
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") { expand("version" to pluginVersion) }
}
tasks.test {
    useJUnitPlatform()
    jvmArgs("-javaagent:" + mockitoAgent.singleFile.absolutePath, "--enable-native-access=ALL-UNNAMED")
}
dependencyLocking { lockAllConfigurations() }
tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
}
tasks.jar { archiveClassifier.set("plain") }
tasks.build { dependsOn(tasks.shadowJar) }

val releaseBundle by tasks.registering(Zip::class) {
    dependsOn(tasks.shadowJar)
    archiveBaseName.set("GGWPWildlands")
    archiveClassifier.set("release")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    from(tasks.shadowJar)
    from("README.md", "SPEC.md", "CROSSPLAY-COMPATIBILITY.md", "MILESTONES.md")
    from(projectDir) { include("MILESTONE-*-REVIEW.md") }
    from("docs") { into("docs") }
    from("assets") { into("assets") }
    from("src/main/resources") { include("*.yml"); exclude("plugin.yml"); into("config-examples") }
}
tasks.build { dependsOn(releaseBundle) }
