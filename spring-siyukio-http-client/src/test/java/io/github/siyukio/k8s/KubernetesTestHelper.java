package io.github.siyukio.k8s;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Kubernetes test helper for running tests as K8s Jobs.
 *
 * @author Bugee
 */
@Slf4j
public class KubernetesTestHelper implements AutoCloseable {

    private static final long EXEC_TIMEOUT_SECONDS = 60;
    @Getter
    private final KubernetesClient client;
    @Getter
    private final String namespace;
    @Getter
    private final String podName;
    @Getter
    private final String imageName;
    private final String containerName;


    public KubernetesTestHelper(String podName, String imageName) {
        this(podName, imageName, "default");
    }

    public KubernetesTestHelper(String podName, String imageName, String namespace) {
        this.podName = podName;
        this.imageName = imageName;
        this.namespace = namespace;
        this.containerName = podName;
        this.client = new KubernetesClientBuilder().build();
    }

    public static void main(String[] args) throws Exception {
        String imageName = args.length > 0 ? args[0] : "maven:3.9-eclipse-temurin-21";

        try (KubernetesTestHelper helper = new KubernetesTestHelper("siyukio-test-pod", imageName)) {
            // Create a persistent pod
            helper.createPod();

            // Wait for pod to be ready
            helper.waitForPodReady();

            // Copy project to pod - use Maven project root (user.dir)
            String projectRoot = System.getProperty("user.dir");
            log.info("Uploading project from: {}", projectRoot);
            helper.uploadDirectory(Path.of(projectRoot), "/app");

            // Execute command in pod
            String command = "cd /app && mvn test -Dgroups=k8s -DskipTests=false -pl spring-siyukio-http-client -am";
            ExecResult result = helper.execInPod("sh", "-c", command);
            log.info("Exit code: {}", result.exitCode());
        }
    }

    public void createPod() {
        log.info("Creating pod: {}", podName);

        var pod = new PodBuilder()
                .withNewMetadata()
                .withName(podName)
                .addToLabels("test", "true")
                .endMetadata()
                .withNewSpec()
                .addToContainers(new ContainerBuilder()
                        .withName(containerName)
                        .withImage(imageName)
                        .withImagePullPolicy("IfNotPresent")
                        .withCommand("sleep", "infinity")
                        .withVolumeMounts(new VolumeMount().edit()
                                .withMountPath("/root/.m2")
                                .withName("maven-repo")
                                .build())
                        .build())
                .addToVolumes(new Volume().edit()
                        .withName("maven-repo")
                        .withHostPath(new HostPathVolumeSource("/data/m2-cache", "DirectoryOrCreate"))
                        .build())
                .endSpec()
                .build();

        client.pods()
                .inNamespace(namespace)
                .resource(pod).serverSideApply();

        log.info("Pod created: {}", podName);
    }

    public void waitForPodReady() throws InterruptedException {
        log.info("Waiting for pod {} to be ready...", podName);
        PodResource podResource = client.pods().inNamespace(namespace).withName(podName);

        int retries = 60;
        while (retries > 0) {
            var pod = podResource.get();
            if (pod != null && "Running".equals(pod.getStatus().getPhase())) {
                log.info("Pod is ready!");
                return;
            }
            Thread.sleep(1000);
            retries--;
        }
        throw new RuntimeException("Pod did not become ready in time");
    }

    /**
     * Upload a directory to the pod.
     */
    public void uploadDirectory(Path localDir, String remotePath) throws Exception {
        log.info("Uploading directory {} to {}:{}", localDir, podName, remotePath);


        // Create tar archive of the directory
        Path tarPath = Files.createTempFile("upload", ".tar");
        try {
            ProcessBuilder pb = new ProcessBuilder("tar", "-cf", tarPath.toString(),
                    "-C", localDir.toString(), "pom.xml",
                    "spring-siyukio-tools",
                    "spring-siyukio-postgresql",
                    "spring-siyukio-application",
                    "spring-siyukio-http-client",
                    "spring-siyukio-application-acp",
                    "spring-siyukio-acp-client");
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to create tar archive, exit code: " + exitCode);
            }

            // Upload tar and extract in pod
            try (FileInputStream fis = new FileInputStream(tarPath.toFile())) {
                client.pods()
                        .inNamespace(namespace)
                        .withName(podName)
                        .file(tarPath.toString())
                        .upload(fis);
            }

            // Extract tar in pod
            execInPod("mkdir", "-p", remotePath);
            execInPod("tar", "-xf", tarPath.toString(), "-C", remotePath);

            log.info("Directory uploaded successfully");
        } finally {
            Files.deleteIfExists(tarPath);
        }
    }

    /**
     * Execute a command in the pod and return the result.
     */
    public ExecResult execInPod(String... command) throws IOException {
        log.info("Executing in pod {}: {}", podName, String.join(" ", command));
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        OutputStream outputStream = new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                char c = (char) b;
                if (c == '\n') {
                    System.out.println("Pod: " + buffer);
                    buffer.setLength(0);
                } else {
                    buffer.append(c);
                }
            }
        };

        try (ExecWatch watch = client.pods()
                .inNamespace(namespace)
                .withName(podName)
                .inContainer(containerName)
                .writingOutput(outputStream)
                .writingError(errorStream)
                .exec(command)) {

            // Wait for exit code
            Integer exitCode = watch.exitCode().get(EXEC_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            return new ExecResult(exitCode != null ? exitCode : -1, errorStream.toString());
        } catch (Exception e) {
            log.error("Failed to execute command: {}", e.getMessage());
            return new ExecResult(-1, e.getMessage());
        }
    }

    public void cleanup() {
        log.info("Cleaning up resources...");

        try {
            client.pods().inNamespace(namespace).withName(podName).delete();
            log.info("Deleted pod: {}", podName);
        } catch (Exception e) {
            log.warn("Failed to delete pod: {}", e.getMessage());
        }

        log.info("Cleanup completed!");
    }

    @Override
    public void close() {
        cleanup();
        if (client != null) {
            client.close();
        }
    }

    /**
     * Result of a command execution.
     */
    public record ExecResult(
            int exitCode,
            String error
    ) {
    }
}
