plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}
group = "gg.ggwp.wildlands"
version = "0.10.0-prototype.1"
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://repo.opencollab.dev/maven-releases/")
}
val geyserBridge = sourceSets.create("geyserBridge")
configurations.named(geyserBridge.compileClasspathConfigurationName) {
    resolutionStrategy.force("org.geysermc.event:events:1.1-20230815.153219-4")
}
dependencies {
    add(geyserBridge.compileOnlyConfigurationName, "org.geysermc.geyser:api:2.11.3-20261006.093555-15")
    testImplementation(geyserBridge.output)
    testImplementation(files(geyserBridge.compileClasspath))
}
val geyserBridgeJar by tasks.registering(Jar::class) {
    archiveBaseName.set("Wildlands-Geyser-Models")
    from(geyserBridge.output)
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
val javaWildlifePack by tasks.registering(Zip::class) {
    archiveBaseName.set("Wildlands-Java-Wildlife")
    destinationDirectory.set(layout.buildDirectory.dir("packs"))
    from("assets/java/pack")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
val bedrockWildlifePack by tasks.registering(Zip::class) {
    archiveBaseName.set("Wildlands-Bedrock-Wildlife")
    archiveExtension.set("mcpack")
    destinationDirectory.set(layout.buildDirectory.dir("packs"))
    from("assets/bedrock/pack")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
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
// Timestamped Maven snapshots normalize their metadata version to -SNAPSHOT;
// locking that normalized name conflicts with the immutable timestamp selector.
configurations.named(geyserBridge.compileClasspathConfigurationName) { resolutionStrategy.deactivateDependencyLocking() }
tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
}
tasks.jar { archiveClassifier.set("plain") }
tasks.build { dependsOn(tasks.shadowJar) }

val releaseBundle by tasks.registering(Zip::class) {
    dependsOn(tasks.shadowJar, geyserBridgeJar, javaWildlifePack, bedrockWildlifePack)
    archiveBaseName.set("GGWPWildlands")
    archiveClassifier.set("release")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    from(tasks.shadowJar)
    from(geyserBridgeJar) { into("optional-geyser-extension") }
    from(javaWildlifePack) { into("packs") }
    from(bedrockWildlifePack) { into("packs") }
    from("README.md", "SPEC.md", "CROSSPLAY-COMPATIBILITY.md", "MILESTONES.md")
    from(projectDir) { include("MILESTONE-*-REVIEW.md") }
    from("docs") { into("docs") }
    from("assets") { into("assets") }
    from("tools") { include("generate_wildlife_assets.py", "asset-requirements.txt"); into("asset-tools") }
    from("src/main/resources") { include("*.yml"); exclude("plugin.yml"); into("config-examples") }
}
tasks.build { dependsOn(releaseBundle) }
