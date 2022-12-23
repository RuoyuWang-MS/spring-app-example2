package hello;

import java.lang.management.ManagementFactory;
import java.time.Duration;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

@RestController
public class HelloController {

    @Value("${health-check.result}")
    private String healthCheckResult;

    @Value("${env.var.1:bad}")
    private String env1;

    @Value("${azure.spring.cloud.config-server.health:DOWN}")
    private String configServerHealth;

    @Value("${azure.spring.cloud.config-server.secret:UNKNOWN}")
    private String configServerSecret;

    @Value("${azure.keyvault.uri:local}")
    private String keyVaultUrl;

    private SecretClient secretClient;

    @PostConstruct
    private void setupSecretClient() {
        ManagedIdentityCredential managedIdentityCredential = new ManagedIdentityCredentialBuilder()
                .maxRetry(1)
                .retryTimeout(duration -> Duration.ofMinutes(1))
                .build();

        secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUrl)
                .credential(managedIdentityCredential)
                .buildClient();
    }


    @RequestMapping("/health")
    public String index() {
        return healthCheckResult;
    }

    @RequestMapping("/env1")
    public String env1() {
        return env1;
    }

    @RequestMapping("/config-server/health")
    public String configServerHealth() {
        return configServerHealth;
    }

    @RequestMapping("/config-server/secret")
    public String configServerSecret() {
        return configServerSecret;
    }
    
    @RequestMapping("/maxHeapSize")
    public long maxHeapSize() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax() / 1024 / 1024;
    }

    @RequestMapping("/initHeapSize")
    public long minHeapSize() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getInit() / 1024 / 1024;
    }

    @PutMapping("/secrets/{name}")
    public String setSecret(@PathVariable String name, @RequestParam String value) {
        try {
            KeyVaultSecret secret = secretClient.setSecret(name, value);
            return String.format("Successfully set secret %s in Key Vault %s", name, keyVaultUrl);
        } catch (Exception ex) {
            return String.format("Failed to set secret %s in Key Vault %s due to %s", name,
                    keyVaultUrl, ex.getMessage());
        }
    }

    @GetMapping(path="/secrets/{name}")
    public String getSecret(@PathVariable String name) {
        try {
            KeyVaultSecret secret = secretClient.getSecret(name);
            return secret.getValue();
        } catch (Exception ex) {
            return String.format("Failed to get secret %s from Key Vault %s due to %s", name,
                    keyVaultUrl, ex.getMessage());
        }
    }
}
