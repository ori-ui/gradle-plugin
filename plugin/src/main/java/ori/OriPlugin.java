package ori;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.JavaVersion;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.os.OperatingSystem;

import com.android.build.api.dsl.ApplicationExtension;
import com.android.build.api.dsl.ApkSigningConfig;
import com.android.build.api.dsl.SdkComponents;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Properties;

public class OriPlugin implements Plugin<Project> {
    static Map<String, String> targets = Map.of(
            "arm64-v8a", "aarch64-linux-android",
            "armabi-v7a", "arm7-linux-androideabi",
            "x86_64", "x86_64-linux-android",
            "x86", "i686-linux-android");

    @Override
    public void apply(Project project) {
        try {
            CargoMetadata meta = new CargoMetadata(project);

            project.getPlugins().withId(
                    "com.android.application",
                    plugin -> configureAndroid(project, meta));

            TaskProvider<Copy> debugTask = registerTasks(project, meta, "Debug", "dev");
            TaskProvider<Copy> releaseTask = registerTasks(project, meta, "Release", "release");

            AndroidComponentsExtension<?, ?, ?> components = project.getExtensions()
                    .getByType(AndroidComponentsExtension.class);

            components.onVariants(
                    components.selector().all(),
                    variant -> {
                        if (variant.getName().equals("release")) {
                            variant.getLifecycleTasks().registerPreBuild(releaseTask);
                        } else {
                            variant.getLifecycleTasks().registerPreBuild(debugTask);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to read cargo metadata", e);
        }
    }

    @SuppressWarnings("deprecation")
    private void configureAndroid(Project project, CargoMetadata meta) {
        project.getDependencies().add(
                "implementation",
                "ori:activity:1.0.0");

        ApplicationExtension android = project.getExtensions().getByType(ApplicationExtension.class);

        android.setCompileSdk(35);
        android.setNamespace(meta.namespace);
        android.setNdkVersion("27.3.13750724");

        android.getDefaultConfig().setApplicationId(meta.applicationId);
        android.getDefaultConfig().setVersionName(meta.version);
        android.getDefaultConfig().setVersionCode(meta.versionCode);
        android.getDefaultConfig().setMinSdk(21);
        android.getDefaultConfig().setTargetSdk(35);
        android.getDefaultConfig().getManifestPlaceholders().put("appLabel", meta.label);

        android.getCompileOptions().setSourceCompatibility(JavaVersion.VERSION_17);
        android.getCompileOptions().setTargetCompatibility(JavaVersion.VERSION_17);

        String storeFile = System.getenv("ANDROID_KEYSTORE");
        String storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD");
        String keyAlias = System.getenv("ANDROID_KEY_ALIAS");
        String keyPassword = System.getenv("ANDROID_KEY_PASSWORD");

        File propertiesFile = project.getRootProject().file(meta.keyProperties);

        if (propertiesFile.exists()) {
            Properties properties = new Properties();

            try (FileInputStream fs = new FileInputStream(propertiesFile)) {
                properties.load(fs);

                storeFile = properties.getProperty("storeFile");
                storePassword = properties.getProperty("storePassword");
                keyAlias = properties.getProperty("keyAlias");
                keyPassword = properties.getProperty("keyPassword");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        ApkSigningConfig signing = android.getSigningConfigs().maybeCreate("release");

        if (storeFile != null) {
            signing.setStoreFile(project.file(storeFile));
        }

        signing.setStorePassword(storePassword);
        signing.setKeyAlias(keyAlias);
        signing.setKeyPassword(keyPassword);

        android.getBuildTypes().getByName("release").setSigningConfig(signing);

        String jniLibsFile = project
                .getLayout()
                .getBuildDirectory()
                .dir("generated/jniLibs")
                .get()
                .toString();

        android.getSourceSets().getByName("main").getJniLibs().srcDirs(jniLibsFile);
    }

    private TaskProvider<Copy> registerTasks(
            Project project,
            CargoMetadata meta,
            String variant,
            String profile) {
        List<TaskProvider<Exec>> buildTasks = new ArrayList<TaskProvider<Exec>>();

        targets.forEach((abi, triple) -> {
            if (!meta.targets.contains(abi))
                return;

            buildTasks.add(registerBuildTask(project, meta, variant, profile, abi, triple));
        });

        String buildTaskName = "cargoBuild" + variant;
        project.getTasks().register(buildTaskName, task -> {
            task.setGroup("rust");
            task.setDescription("Build Rust (" + variant + ") for (all ABIs).");

            task.dependsOn(buildTasks);
        });

        String copyTaskName = "copyJniLibs" + variant;
        return project.getTasks().register(copyTaskName, Copy.class, task -> {
            task.setGroup("rust");
            task.setDescription("Copy compiled Rust (" + variant + ") objects.");

            task.into(project.getLayout().getBuildDirectory().dir("generated/jniLibs"));

            targets.forEach((abi, target) -> {
                if (!meta.targets.contains(abi))
                    return;

                task.from(new File(meta.targetDirectory, "/" + target + "/" + variant.toLowerCase()),
                        spec -> {
                            spec.include("*.so");
                            spec.rename(name -> "libnative.so");
                            spec.into(abi);
                        });
            });

            task.dependsOn(buildTasks);
        });
    }

    private TaskProvider<Exec> registerBuildTask(
            Project project,
            CargoMetadata meta,
            String variant,
            String profile,
            String abi,
            String triple) {
        ApplicationExtension android = project.getExtensions().getByType(ApplicationExtension.class);
        SdkComponents sdkComponents = project.getExtensions()
                .getByType(AndroidComponentsExtension.class)
                .getSdkComponents();

        String taskName = "cargoBuild"
                + variant
                + abi.substring(0, 1).toUpperCase()
                + abi.substring(1);

        return project.getTasks().register(taskName, Exec.class, task -> {
            task.setGroup("rust");

            task.setDescription("Build Rust (" + variant + ") for [" + abi + "].");

            File rustDir = new File(project.getRootDir(), "..");

            task.setWorkingDir(rustDir);

            task.commandLine(
                    "cargo",
                    "build",
                    "--lib",
                    "--target", triple,
                    "--profile", profile,
                    "--color", "always");

            String host;
            if (OperatingSystem.current().isWindows()) {
                host = "window-x86_64";
            } else if (OperatingSystem.current().isMacOsX()) {
                host = "darwin-x86_64";
            } else {
                host = "linux-x86_64";
            }

            String envTriple = triple.toUpperCase().replace("-", "_");
            File clang = new File(triple + android.getDefaultConfig().getMinSdk() + "-clang");
            File llvm = new File(sdkComponents.getNdkDirectory().get().toString(),
                    "toolchains/llvm/prebuilt/" + host + "/bin");

            task.environment("ANDROID_SDK_ROOT", sdkComponents.getSdkDirectory());
            task.environment("ANDROID_NDK", sdkComponents.getNdkDirectory());

            task.environment("CARGO_TARGET_" + envTriple + "_LINKER",
                    new File(llvm, clang.toString()));
            task.environment("CARGO_TARGET_" + envTriple + "_AR",
                    new File(llvm, "llvm-ar"));

            task.environment("CC_" + envTriple,
                    new File(llvm, clang.toString()));
            task.environment("CXX_" + envTriple,
                    new File(llvm, clang.toString() + "++"));

            task.environment("CC", new File(llvm, clang.toString()));
            task.environment("CXX", new File(llvm, clang.toString() + "++"));
        });
    }

}

class CargoMetadata {
    String targetDirectory;

    Set<String> targets = new HashSet<>();

    String label;
    String namespace;
    String applicationId;
    String version;
    int versionCode = 1;

    String keyProperties = "key.properties";

    public CargoMetadata(Project project) throws IOException {
        String metaString = readMetadata(project);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode cargoMeta = mapper.readTree(metaString);

        targetDirectory = cargoMeta.get("target_directory").asText();

        JsonNode pkg = null;
        for (var p : cargoMeta.get("packages")) {
            String defaultMember = cargoMeta
                    .get("workspace_default_members")
                    .get(0)
                    .asText();

            if (p.get("id").asText().equals(defaultMember)) {
                pkg = p;
            }
        }

        if (pkg == null) {
            throw new RuntimeException("No default cargo workspaces defined");
        }

        JsonNode meta = pkg.get("metadata").get("android");

        if (meta == null) {
            throw new RuntimeException("Android metadata not found in `Cargo.toml`");
        }

        if (meta.get("targets") != null) {
            for (var target : meta.get("targets")) {
                targets.add(target.asText());
            }
        } else {
            targets.add("arm64-v8a");
            targets.add("x86_64");
        }

        if (meta.get("name") != null) {
            label = meta.get("name").asText();
        } else {
            label = pkg.get("name").asText();
        }

        namespace = meta.get("package").asText();
        applicationId = meta.get("package").asText();

        if (meta.get("version") != null) {
            version = meta.get("version").asText();
        } else {
            version = pkg.get("version").asText();
        }

        if (meta.get("version-code") != null) {
            versionCode = meta.get("version-code").asInt();
        }

        if (meta.get("key-properties") != null) {
            keyProperties = meta.get("key-properties").asText();
        }
    }

    private static String readMetadata(Project project) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "cargo",
                "metadata",
                "--format-version",
                "1",
                "--no-deps");

        processBuilder.directory(project.getRootDir());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        StringBuilder output = new StringBuilder();
        while ((line = reader.readLine()) != null)
            output.append(line);

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return output.toString();
    }
}
